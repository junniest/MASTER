/*
 * Copyright (C) 2012 Tim Vaughan <tgvaughan@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package master.examples;

import beast.math.Binomial;
import master.EnsembleSummary;
import master.EnsembleSummarySpec;
import master.JsonOutput;
import master.Model;
import master.Moment;
import master.MomentGroup;
import master.Population;
import master.PopulationState;
import master.PopulationType;
import master.Reaction;
import master.ReactionGroup;
import master.TauLeapingStepper;
import java.io.FileNotFoundException;
import java.io.PrintStream;

/**
 * Model of within-host HIV evolution, including APOBEC3*-driven hyper-mutation.
 *
 * @author Tim Vaughan
 */
public class HypermutHIV {

    public static void main(String[] argv) throws FileNotFoundException {

        /*
         * Assemble model:
         */

        Model model = new Model();

        // Sequence space parameters:

        // Sequence length excluding sites belonging to hypermutable motifs:
        int L = 1000;

        // Total number of hypermutable motifs:
        int La3 = 20;

        // Truncation Hamming distance:
        int hTrunc = 20;

        // Reduced sequence space dimension:
        int[] dims = {hTrunc+1, La3+1};

        // Define populations:

        // Uninfected cell:
        Population X = new Population("X");
        model.addPopulation(X);

        // Infected cell:
        PopulationType Ytype = new PopulationType("Y", dims);
        model.addPopulationType(Ytype);

        // Virion:
        PopulationType Vtype = new PopulationType("V", dims);
        model.addPopulationType(Vtype);

        // Define reactions:

        // 0 -> X
        Reaction cellBirth = new Reaction();
        cellBirth.setReactantSchema();
        cellBirth.setProductSchema(X);
        cellBirth.setRate(2.5e8);
        model.addReaction(cellBirth);

        // X + V -> Y (with RT mutation)
        ReactionGroup infection = new ReactionGroup();

        double mu = 2e-5*L; // Mutation probabability per infection event.
        double beta = 5e-13; // Total infection rate.

        for (int ha = 0; ha<=La3; ha++) {

            for (int h = 0; h<=hTrunc; h++) {

                Population V = new Population(Vtype, h, ha);

                int hpmin = h>1 ? h-1 : 0;
                int hpmax = h<hTrunc ? h+1 : hTrunc;

                for (int hp = hpmin; hp<=hpmax; hp++) {

                    Population Y = new Population(Ytype, hp, ha);

                    // Transition rate to hp from a given sequence in h:
                    double rate = mu*gcond(h, hp, L)/(3.0*L);

                    // Mutation-free contribution:
                    if (h==hp)
                        rate += (1-mu);

                    // Incorporate base infection rate:
                    rate *= beta;

                    infection.addReactantSchema(X, V);
                    infection.addProductSchema(Y);
                    infection.addRate(rate);
                }
            }
        }

        model.addReactionGroup(infection);

        // X + V -> Y (with hypermutation)
        ReactionGroup infectionHyper = new ReactionGroup();

        // A3G incorporation probability:
        double pIncorp = 1e-7;

        // A3G hypermutable site mutation probability:
        double pHypermutate = 0.5;

        for (int h = 0; h<=hTrunc; h++) {

            for (int ha = 0; ha<=La3; ha++) {

                Population V = new Population(Vtype, h, ha);
                /* 
                 * Once APOBEC attaches to a sequence with ha remaining
                 * hypermutable sites, it has a finite probability of editing
                 * each one of these sites. Thus the total number of edited
                 * sites delta following any attachment of APOBEC is
                 * binomially distributed.
                 */

                for (int delta = 0; delta<=La3-ha; delta++) {

                    Population Y = new Population(Ytype, h, ha+delta);

                    double rate = beta*pIncorp
                            *Math.pow(Binomial.choose(La3-ha, delta), 2.0)
                            *Math.pow(pHypermutate, delta)
                            *Math.pow(1.0-pHypermutate, La3-ha-delta);

                    infectionHyper.addReactantSchema(X, V);
                    infectionHyper.addProductSchema(Y);
                    infectionHyper.addRate(rate);

                }

            }
        }

        model.addReactionGroup(infectionHyper);

        // Y -> Y + V
        ReactionGroup budding = new ReactionGroup();
        for (int h = 0; h<=hTrunc; h++) {
            for (int ha = 0; ha<=La3; ha++) {

                Population Y = new Population(Ytype, h, ha);
                Population V = new Population(Vtype, h, ha);

                budding.addReactantSchema(Y);
                budding.addProductSchema(Y, V);
            }
        }
        budding.setGroupRate(1e3);
        model.addReactionGroup(budding);

        // X -> 0
        Reaction cellDeath = new Reaction();
        cellDeath.setReactantSchema(X);
        cellDeath.setProductSchema();
        cellDeath.setRate(1e-3);
        model.addReaction(cellDeath);

        // Y -> 0
        ReactionGroup infectedDeath = new ReactionGroup();

        for (int h = 0; h<=hTrunc; h++) {
            for (int ha = 0; ha<=La3; ha++) {
                Population Y = new Population(Ytype, h, ha);

                infectedDeath.addReactantSchema(Y);
                infectedDeath.addProductSchema();
            }
        }
        infectedDeath.setGroupRate(1.0);
        model.addReactionGroup(infectedDeath);

        // V -> 0
        ReactionGroup virionDeath = new ReactionGroup();

        for (int h = 0; h<=hTrunc; h++) {
            for (int ha = 0; ha<=La3; ha++) {
                Population V = new Population (Vtype, h, ha);

                virionDeath.addReactantSchema(V);
                virionDeath.addProductSchema();
            }
        }
        virionDeath.setGroupRate(3.0);
        model.addReactionGroup(virionDeath);

        /*
         * Define moments:
         */

        Moment mX = new Moment("X", X);
        MomentGroup mY = new MomentGroup("Y");
        MomentGroup mV = new MomentGroup("V");

        for (int totMut = 0; totMut<=hTrunc+La3; totMut++) {
            mY.newSum();
            mV.newSum();

            for (int h = 0; h<=hTrunc; h++) {

                int ha = totMut-h;

                if (ha>=0 && ha<=La3) {

                    Population Y = new Population(Ytype, h, ha);
                    mY.addSubSchemaToSum(Y);

                    Population V = new Population(Vtype, h, ha);
                    mV.addSubSchemaToSum(V);
                }
            }
        }

        /*
         * Set initial state:
         */

        PopulationState initState = new PopulationState();

        initState.set(X, 6.006e9); // Deterministic steady state values
        initState.set(new Population(Ytype, 0, 0), 2.44e8);
        initState.set(new Population(Vtype, 0, 0), 8.125e10);

        // Note: unspecified population sizes default to zero.

        /*
         * Define simulation:
         */

        EnsembleSummarySpec spec = new EnsembleSummarySpec();

        spec.setModel(model);
        spec.setSimulationTime(365);
        spec.setStepper(new TauLeapingStepper(365.0/10000.0));
        spec.setEvenSampling(1001);
        spec.setnTraj(1);
        spec.setSeed(53);
        spec.setInitPopulationState(initState);
        spec.addMoment(mX);
        spec.addMomentGroup(mY);
        spec.addMomentGroup(mV);

        // Turn on verbose reportage:
        spec.setVerbosity(2);

        /*
         * Generate ensemble:
         */

        EnsembleSummary ensemble = new EnsembleSummary(spec);

        /*
         * Dump results to file (JSON):
         */

        JsonOutput.write(ensemble, new PrintStream("out.json"));

    }

    /**
     * Return the number of sequences s2 satisfying d(s2,0)=h2 and d(s2,s1)=1
     * where s1 is a particular sequence satisfying d(s1,0)=h1.
     *
     * @param h1
     * @param h2
     * @param L
     * @return
     */
    static int gcond(int h1, int h2, int L) {

        int result;

        switch (h2-h1) {
            case 1:
                result = 3*(L-h1);
                break;
            case 0:
                result = 2*h1;
                break;
            case -1:
                result = h1;
                break;
            default:
                result = 0;
        }

        return result;
    }
}