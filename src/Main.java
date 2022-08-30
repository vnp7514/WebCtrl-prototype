import io.github.cdimascio.dotenv.Dotenv;

import java.util.ArrayList;
import java.util.HashMap;

public class Main {

    public static void main(String args[]) {
        // The excel that contains the slope and intercept to be used
        String preRetrofit = System.getProperty("user.dir") + "/src/TEST.xlsx";
        // The excel that contains the information of the year to be compared to the baseline
        String postRetrofit = System.getProperty("user.dir") + "/src/TEST2.xlsx";
        // The excel that will contains the baseLine comparison
        String output = System.getProperty("user.dir") + "/src/baseline.xlsx";

        APICALL.saveToExcel(preRetrofit);
        util.baseLineTask(preRetrofit);
        line_chart.makeBaseLineSheet(preRetrofit, line_chart.graphBaseLine(preRetrofit));
        line_chart.graphBaselinePerDay(preRetrofit);
        System.out.println("-----------------------------------------------------------------------------------------" +
                "\nFinish creating the file for baseline\n" +
                "----------------------------------------------------------------------------------------------------\n\n");

        APICALL.saveToExcel(postRetrofit);
        util.baseLineTask(postRetrofit);
        line_chart.makeBaseLineSheet(postRetrofit, line_chart.graphBaseLine(postRetrofit));
        line_chart.graphBaselinePerDay(postRetrofit);
        System.out.println("-----------------------------------------------------------------------------------------" +
                "\nFinish creating the file for compare\n" +
                "----------------------------------------------------------------------------------------------------\n\n");

        System.out.println("Starting to create baseLine chart\n");
        Baseline.calculateBaseline(preRetrofit, postRetrofit, output);
        System.out.println("Done");

    }
}
