package master.xmltests;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
public class HIVTest extends XMLTestCase {

    @Override
    public String getXMLFileName() {
        return "examples/HIV.xml";
    }

    @Override
    public long getSeed() {
        return 1;
    }

    @Override
    public Map<File, String> getOutputFileHashes() {
        Map<File, String> map = new HashMap<>();
        map.put(new File("HIV_output_cond.json"), "5ac6aebeae6d3488c26438b0ec3f68b6");
        return map;
    }
}
