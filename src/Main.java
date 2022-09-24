import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import io.github.cdimascio.dotenv.Dotenv;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

public class Main {

    private static void syncOccupancy(Worksheet occupancy, Worksheet energy){
        TreeMap<Integer, TreeMap<LocalTime, Integer>> sisOccupancyTablesMapping =
                Occupancy_Profile.getOccupancyTablesMappingsFromExcel(occupancy,Occupancy_Profile.occupancyProfileSIS, 0);
        TreeMap<Integer, TreeMap<LocalTime, Integer>> reportOccupancyTablesMapping =
                Occupancy_Profile.getOccupancyTablesMappingsFromExcel(occupancy,Occupancy_Profile.occupancyProfileReport, 0);
        TreeMap<Integer, TreeMap<LocalTime, Integer>> trendOccupancyTablesMapping =
                Occupancy_Profile.getOccupancyTablesMappingsFromExcel(occupancy,Occupancy_Profile.occupancyProfileTrend, 0);
        util.syncOccupancyTableDataWithEnergyData(energy, "SIS Occupancy", sisOccupancyTablesMapping, util.SISOccupancyEnergyCol);
        util.syncOccupancyTableDataWithEnergyData(energy, "Trend Occupancy", trendOccupancyTablesMapping, util.TrendOccupancyEnergyCol);
        util.syncOccupancyTableDataWithEnergyData(energy, "Report Occupancy", reportOccupancyTablesMapping, util.ReportOccupancyEnergyCol);
    }

    private static void oatVsEnergyBasedOnOccupancyValue(Worksheet energyWorksheet, Worksheet output){
        TreeMap<Integer, TreeMap<LocalDateTime, ArrayList<Double>>> sisMappings = util.createOATvsEnergyBasedOnOccupancyValue(energyWorksheet, util.SISOccupancyEnergyCol);
        TreeMap<Integer, TreeMap<LocalDateTime, ArrayList<Double>>> trendMappings = util.createOATvsEnergyBasedOnOccupancyValue(energyWorksheet, util.TrendOccupancyEnergyCol);
        TreeMap<Integer, TreeMap<LocalDateTime, ArrayList<Double>>> reportMappings = util.createOATvsEnergyBasedOnOccupancyValue(energyWorksheet, util.ReportOccupancyEnergyCol);
        util.writeOATvsEnergyToExcel(output, "SIS", sisMappings, 0, 0);
        util.writeOATvsEnergyToExcel(output, "Trend", trendMappings, 0, 15);
        util.writeOATvsEnergyToExcel(output, "Report", reportMappings, 0 ,30);
    }

    public static void main(String args[]) {
        try {
            // The excel that contains the slope and intercept to be used
            String preRetrofitName = System.getProperty("user.dir") + "/src/TEST.xlsx";
            Workbook preRetrofit = new Workbook(preRetrofitName);
            // The excel that contains the information of the year to be compared to the baseline
            String postRetrofitName = System.getProperty("user.dir") + "/src/TEST2.xlsx";
            Workbook postRetrofit = new Workbook(postRetrofitName);
            // The excel that will contains the baseLine comparison
            String outputName = System.getProperty("user.dir") + "/src/baseline.xlsx";
            Workbook output = new Workbook();
            String occupancyName = System.getProperty("user.dir") + "/src/OCCUPANCY.xlsx";
            Workbook occupancyWorkbook = new Workbook(occupancyName);
            Worksheet occupancyWorksheet = util.getWorksheetFromWorkbook(occupancyWorkbook, "AHUOccupancyProfile");
            syncOccupancy(occupancyWorksheet, util.getWorksheetFromWorkbook(preRetrofit, "Energy"));
            oatVsEnergyBasedOnOccupancyValue(util.getWorksheetFromWorkbook(preRetrofit, "Energy"), util.getWorksheetFromWorkbook(preRetrofit, "oatVsEnergy"));


//            APICALL.saveToExcel(preRetrofit);
//            util.baseLineTask(preRetrofit);
//            line_chart.makeBaseLineSheet(preRetrofit, line_chart.graphBaseLine(preRetrofit));
//            line_chart.graphBaselinePerDay(preRetrofit);
//            syncOccupancy(occupancyWorksheet, util.getWorksheetFromWorkbook(preRetrofit, "Energy"));
//            System.out.println("-----------------------------------------------------------------------------------------" +
//                    "\nFinish creating the file for baseline\n" +
//                    "----------------------------------------------------------------------------------------------------\n\n");
//
//            APICALL.saveToExcel(postRetrofit);
//            util.baseLineTask(postRetrofit);
//            line_chart.makeBaseLineSheet(postRetrofit, line_chart.graphBaseLine(postRetrofit));
//            line_chart.graphBaselinePerDay(postRetrofit);
            syncOccupancy(occupancyWorksheet, util.getWorksheetFromWorkbook(postRetrofit, "Energy"));
            oatVsEnergyBasedOnOccupancyValue(util.getWorksheetFromWorkbook(postRetrofit, "Energy"), util.getWorksheetFromWorkbook(postRetrofit, "oatVsEnergy"));
//            System.out.println("-----------------------------------------------------------------------------------------" +
//                    "\nFinish creating the file for compare\n" +
//                    "----------------------------------------------------------------------------------------------------\n\n");
//
//            System.out.println("Starting to create baseLine chart\n");
//            Baseline.calculateBaseline(preRetrofit, postRetrofit, output);
//            System.out.println("Done");


            preRetrofit.save(preRetrofitName);
            postRetrofit.save(postRetrofitName);
            output.save(outputName);
        }
        catch (Exception e){
            System.err.println(e.getMessage() + " in main");
        }
    }
}
