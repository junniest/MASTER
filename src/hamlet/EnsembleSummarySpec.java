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
package hamlet;

import java.util.*;

/**
 * Specification for generating a summarised ensemble of birth-death
 * trajectories.
 *
 * @author Tim Vaughan
 */
public class EnsembleSummarySpec extends EnsembleSpec {

    // Moments estimates to record:
    List<MomentGroup> momentGroups;

    public EnsembleSummarySpec() {
        super();

        // Create empty moment list:
        momentGroups = new ArrayList<MomentGroup>();
    }

    /**
     * Add group of moments to estimate.
     *
     * @param momentGroup
     */
    public void addMomentGroup(MomentGroup momentGroup) {
        momentGroup.postSpecInit();
        momentGroups.add(momentGroup);
    }

    /**
     * Add a single moment to estimate.
     *
     * @param moment
     */
    public void addMoment(Moment moment) {
        moment.postSpecInit();
        momentGroups.add(moment);
    }

    @Override
    public void setUnevenSampling() {
        throw new UnsupportedOperationException("Uneven sampling not "
                +"supported for ensemble summaries.");
    }
    
    @Override
    public void addPopSizeEndCondition(PopulationEndCondition endCondition) {
        if (!endCondition.isRejection())
            throw new IllegalArgumentException("Only rejection end conditions"
                    + " allowed for ensemble summary calculations.");
        
        super.addPopSizeEndCondition(endCondition);
    }
}
