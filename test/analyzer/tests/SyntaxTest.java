package analyzer.tests;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.*;
import java.util.*;

/**
 * Created: 17-08-02
 * Author: Nicolas Cloutier
 *
 * Last Changed: 23-09-08
 * Author: Raphaël Tremblay
 *
 * Description: This test the grammar implemented in Grammaire.jjt.
 * So it will basically test that the tree can be written into valid source code.
 */

@RunWith(Parameterized.class)
public class SyntaxTest extends BaseTest {

    private static String m_test_suite_path = "./test-suite/SyntaxTest/data";

    public SyntaxTest(File file) {
        super(file);
    }

    @Test
    public void run() throws Exception {
        runAndAssert();
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getFiles() {
        return getFiles(m_test_suite_path);
    }

}