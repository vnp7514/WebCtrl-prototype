import io.github.cdimascio.dotenv.Dotenv;

import java.util.ArrayList;
import java.util.HashMap;

public class Main {

    public static void main(String args[]) {
        // The excel that contains the slope and intercept to be used in baseline
        String baseLine = System.getProperty("user.dir") + "/src/2021.xlsx";
        // The excel that contains the information of the year to be compared to the baseline
        String compare = System.getProperty("user.dir") + "/src/2022.xlsx";
        // The excel that will contains the baseLine comparison
        String output = System.getProperty("user.dir") + "/src/baseline.xlsx";

        APICALL.saveToExcel(baseLine, APICALL.retrieveData("2021-01-01", "2021-12-31"));
        util.baseLineTask(baseLine);
        line_chart.makeBaseLineSheet(baseLine, line_chart.graphBaseLine(baseLine));
        System.out.println("-----------------------------------------------------------------------------------------" +
                "\nFinish creating the file for baseline\n" +
                "----------------------------------------------------------------------------------------------------\n\n");

        APICALL.saveToExcel(compare, APICALL.retrieveData("2022-01-01", "2022-12-31"));
        util.baseLineTask(compare);
        line_chart.makeBaseLineSheet(compare, line_chart.graphBaseLine(compare));
        System.out.println("-----------------------------------------------------------------------------------------" +
                "\nFinish creating the file for compare\n" +
                "----------------------------------------------------------------------------------------------------\n\n");

        System.out.println("Starting to create baseLine chart\n");
        Baseline.calculateBaseline(baseLine, compare, output);
        System.out.println("Done");

    }
}
