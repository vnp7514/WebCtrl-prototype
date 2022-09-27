import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import io.github.cdimascio.dotenv.Dotenv;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class Main {

    private static void checkParrallelArrays(Collection<ArrayList<Double>> arrays) throws Exception {
        if (arrays.size() < 1){
            return;
        }
        List<ArrayList<Double>> list = arrays.stream().toList();
        int expectedLength = list.get(0).size();
        for (int i = 1; i < list.size(); i++){
            if (expectedLength != list.get(i).size()){
                throw new Exception("Mismatched Array Size!");
            }
        }
    }

    /**
     * This contains all the processes of calculating energy for the provided data in Trend Values Sorted
     * @param preRetrofit can be preRetrofit or postRetrofit
     * @param occupancyWorkbook the workbook contains information about occupancy profiles
     * @throws Exception
     */
    private static void TrendStuffs(Workbook preRetrofit, Workbook occupancyWorkbook) throws Exception {

        Worksheet sortedTrendValues = util.getWorksheetFromWorkbook(preRetrofit, "Trend Values Sorted");
        Worksheet inputForNOAA = util.getWorksheetFromWorkbook(preRetrofit, "Input for Degree Days");
        Worksheet energyTrendValues = util.getWorksheetFromWorkbook(preRetrofit, "Energy");
        Worksheet hourlyTrendValues = util.getWorksheetFromWorkbook(preRetrofit, "Hourly");
        Worksheet dailyTrendWorksheet = util.getWorksheetFromWorkbook(preRetrofit, "Daily");
        Worksheet monthlyTrendWorksheet = util.getWorksheetFromWorkbook(preRetrofit, "Monthly");

        Worksheet occupancyWorksheet = util.getWorksheetFromWorkbook(occupancyWorkbook, "AHUOccupancyProfile");

        // The key of this mapping is the date
        // The value of this mapping is a list of values
        LinkedHashMap<String, ArrayList<String>> sorted = util.getSortedTrendValues(sortedTrendValues);
        // The names associated with each values in the list of values above
        ArrayList<String> names = util.getTrendNames(sortedTrendValues);
        // Contains Heating and Cooling Q values for each date
        LinkedHashMap<String, ArrayList<Double>> qValues = util.calculateQValues(names, sorted);
        // Contains Heating and Cooling Energy (thousand Btu) for each date
        LinkedHashMap<String, ArrayList<Double>> energy = util.calculateEnergy(qValues);

        // STEPS TO PRODUCE ENERGY WORKSHEET
        ArrayList<String> namesForEnergy = new ArrayList<>();
        namesForEnergy.addAll(names);
        namesForEnergy.add("Cooling Q Value (Btu/min)");
        namesForEnergy.add("Heating Q Value (Btu/min)");
        namesForEnergy.add("Cooling Energy (thousand Btu)");
        namesForEnergy.add("Heating Energy (thousand Btu)");
        LinkedHashMap<String, ArrayList<Double>> dataWithQValuesOnly = util.combineStringHashMapAndDoubleHashMap(sorted, qValues);
        LinkedHashMap<String, ArrayList<Double>> allData = util.combineHashMaps(dataWithQValuesOnly, energy);
        util.writeEnergy(energyTrendValues, namesForEnergy, allData, 0, 0);
        syncOccupancy(occupancyWorksheet, util.getWorksheetFromWorkbook(preRetrofit, "Energy"));
        // DONE


        // STEPS TO PRODUCE HOURLY WORKSHEET
        util.calculateAndWriteHourlyData(hourlyTrendValues, namesForEnergy, allData);
        // DONE

        // STEPS TO PRODUCE DAILY WORKSHEET
        util.calculateAndWriteDailyData(dailyTrendWorksheet, inputForNOAA, namesForEnergy, allData, 0, 0);
        // DONE

        util.calculateAndWriteMonthlyData(monthlyTrendWorksheet, inputForNOAA, namesForEnergy, allData, 0, 0);


        oatVsEnergyBasedOnOccupancyValue(util.getWorksheetFromWorkbook(preRetrofit, "Energy"), util.getWorksheetFromWorkbook(preRetrofit, "oatVsEnergy"));
    }

    private static void graph(Workbook workbook){
        Worksheet energyTrendValues = util.getWorksheetFromWorkbook(workbook, "Energy");
        Worksheet hourlyTrendValues = util.getWorksheetFromWorkbook(workbook, "Hourly");
        Worksheet dailyTrendWorksheet = util.getWorksheetFromWorkbook(workbook, "Daily");
        Worksheet monthlyTrendWorksheet = util.getWorksheetFromWorkbook(workbook, "Monthly");
        line_chart.graphDegreeDaysvsEnergy(monthlyTrendWorksheet, "NOAA Cooling Degree Days (℉.day/month)",
                "NOAA Heating Degree Days (℉.day/month)",
                "Cooling Energy (thousand Btu)", "Heating Energy (thousand Btu)");
        line_chart.graphDegreeDaysvsEnergy(dailyTrendWorksheet, "NOAA Cooling Degree Days (℉.day)",
                "NOAA Heating Degree Days (℉.day)",
                "Cooling Energy (thousand Btu)", "Heating Energy (thousand Btu)");
        line_chart.graphTimevsEnergy(dailyTrendWorksheet, "Date","Cooling Energy (thousand Btu)", "Heating Energy (thousand Btu)" );
        line_chart.graphTimevsEnergy(hourlyTrendValues, "Date","Cooling Energy (thousand Btu)", "Heating Energy (thousand Btu)");
        line_chart.graphTimevsEnergy(monthlyTrendWorksheet, "Date", "Cooling Energy (thousand Btu)", "Heating Energy (thousand Btu)");
        line_chart.graphOATvsEnergy(dailyTrendWorksheet, "oa_temp  (℉)","Cooling Energy (thousand Btu)", "Heating Energy (thousand Btu)" );
        line_chart.graphOATvsEnergy(hourlyTrendValues, "oa_temp  (℉)","Cooling Energy (thousand Btu)", "Heating Energy (thousand Btu)");
        line_chart.graphOATvsEnergy(monthlyTrendWorksheet, "oa_temp  (℉)", "Cooling Energy (thousand Btu)", "Heating Energy (thousand Btu)");

    }

    public static void main(String[] args){
        try{
            // The excel that contains the slope and intercept to be used
            String preRetrofitName = System.getProperty("user.dir") + "/src/TEST.xlsx";
            String occupancyName = System.getProperty("user.dir") + "/src/OCCUPANCY.xlsx";
            String postRetrofitName = System.getProperty("user.dir") + "/src/TEST2.xlsx";
            String outputName = System.getProperty("user.dir") + "/src/baseline.xlsx";

            Workbook preRetrofit = new Workbook(preRetrofitName);
            Workbook postRetrofit = new Workbook(postRetrofitName);
            Workbook output = new Workbook();
            Workbook occupancyWorkbook = new Workbook(occupancyName);


            // Pull and Store data from WebCtrl
            // Commented out so we dont constantly pull data from WebCtrl
            util.makeTrendValuesWorksheets(preRetrofit);
            util.makeTrendValuesWorksheets(postRetrofit);

            // Perform all the energy calculations
            TrendStuffs(preRetrofit, occupancyWorkbook);
            TrendStuffs(postRetrofit, occupancyWorkbook);
            graph(preRetrofit);
            graph(postRetrofit);

            baseLineCalculation(preRetrofit, postRetrofit, output);


            preRetrofit.save(preRetrofitName);
            postRetrofit.save(postRetrofitName);
            output.save(outputName);

        } catch(Exception e){
            System.err.println(e.getMessage());
        }
    }

    private static void baseLineCalculation(Workbook preRetrofit, Workbook postRetrofit, Workbook output) throws Exception {
        Worksheet monthlyPreRetrofitWorksheet = util.getWorksheetFromWorkbook(preRetrofit, "Monthly");
        Worksheet monthlyPostRetrofitWorksheet = util.getWorksheetFromWorkbook(postRetrofit, "Monthly");
        Worksheet baselineWorksheet = util.getWorksheetFromWorkbook(output, "Baseline");
        ArrayList<Double> coolingInterceptSlope = line_chart.getInterceptAndSlope(monthlyPreRetrofitWorksheet, line_chart.CDDVSCOOL);
        ArrayList<Double> heatingInterceptSlope = line_chart.getInterceptAndSlope(monthlyPreRetrofitWorksheet, line_chart.HDDVSHEAT);
        LinkedHashMap<String, ArrayList<Double>> heat = line_chart.getMonthDDEnergy(monthlyPostRetrofitWorksheet, "Date", "NOAA Heating Degree Days (℉.day/month)", "Heating Energy (thousand Btu)", 1);
        LinkedHashMap<String, ArrayList<Double>> cool = line_chart.getMonthDDEnergy(monthlyPostRetrofitWorksheet, "Date", "NOAA Cooling Degree Days (℉.day/month)", "Cooling Energy (thousand Btu)", 1);
        Baseline.writeBaseline(baselineWorksheet, "Cooling Monthly",
                new ArrayList<>(Arrays.asList("Date", "NOAA Cooling Degree Days (℉.day/month)", "Intercept", "Slope", "Actual Cooling Energy (thousand Btu)", "Adjusted Cooling Energy (thousand Btu)", "Savings (thousand Btu)")),
                coolingInterceptSlope, cool, 0, 0);
        Baseline.writeBaseline(baselineWorksheet, "Heating Monthly",
                new ArrayList<>(Arrays.asList("Date", "NOAA Heating Degree Days (℉.day/month)", "Intercept", "Slope", "Actual Heating Energy (thousand Btu)", "Adjusted Heating Energy (thousand Btu)", "Savings (thousand Btu)")),
                heatingInterceptSlope, heat, 0, 20);

    }

    public static void normalWorkflow() {
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


            APICALL.saveToExcel(preRetrofit);
            util.baseLineTask(preRetrofit);
//            line_chart.makeBaseLineSheet(preRetrofit, line_chart.graphBaseLine(preRetrofit));
            line_chart.graphBaselinePerDay(preRetrofit);
            System.out.println("-----------------------------------------------------------------------------------------" +
                    "\nFinish creating the file for baseline\n" +
                    "----------------------------------------------------------------------------------------------------\n\n");

            APICALL.saveToExcel(postRetrofit);
            util.baseLineTask(postRetrofit);
//            line_chart.makeBaseLineSheet(postRetrofit, line_chart.graphBaseLine(postRetrofit));
            line_chart.graphBaselinePerDay(postRetrofit);
            syncOccupancy(occupancyWorksheet, util.getWorksheetFromWorkbook(postRetrofit, "Energy"));
            oatVsEnergyBasedOnOccupancyValue(util.getWorksheetFromWorkbook(postRetrofit, "Energy"), util.getWorksheetFromWorkbook(postRetrofit, "oatVsEnergy"));
            System.out.println("-----------------------------------------------------------------------------------------" +
                    "\nFinish creating the file for compare\n" +
                    "----------------------------------------------------------------------------------------------------\n\n");

            System.out.println("Starting to create baseLine chart\n");
            Baseline.calculateBaseline(preRetrofit, postRetrofit, output);
            System.out.println("Done");


            preRetrofit.save(preRetrofitName);
            postRetrofit.save(postRetrofitName);
            output.save(outputName);
        }
        catch (Exception e){
            System.err.println(e.getMessage() + " in normalWorkflow");
        }
    }

    private static void oatVsEnergyBasedOnOccupancyValue(Worksheet energyWorksheet, Worksheet output){
        TreeMap<Integer, TreeMap<LocalDateTime, ArrayList<Double>>> sisMappings = util.createOATvsEnergyBasedOnOccupancyValue(energyWorksheet, util.getColumnIndexOf("SIS Occupancy", energyWorksheet, 0));
        TreeMap<Integer, TreeMap<LocalDateTime, ArrayList<Double>>> trendMappings = util.createOATvsEnergyBasedOnOccupancyValue(energyWorksheet, util.getColumnIndexOf("Trend Occupancy", energyWorksheet, 0));
        TreeMap<Integer, TreeMap<LocalDateTime, ArrayList<Double>>> reportMappings = util.createOATvsEnergyBasedOnOccupancyValue(energyWorksheet, util.getColumnIndexOf("Report Occupancy", energyWorksheet, 0));
        util.writeOATvsEnergyToExcel(output, "SIS", sisMappings, 0, 0);
        util.writeOATvsEnergyToExcel(output, "Trend", trendMappings, 0, 15);
        util.writeOATvsEnergyToExcel(output, "Report", reportMappings, 0 ,30);
    }

    private static void syncOccupancy(Worksheet occupancy, Worksheet energy){
        TreeMap<Integer, TreeMap<LocalTime, Integer>> sisOccupancyTablesMapping =
                Occupancy_Profile.getOccupancyTablesMappingsFromExcel(occupancy,Occupancy_Profile.occupancyProfileSIS, 0);
        TreeMap<Integer, TreeMap<LocalTime, Integer>> reportOccupancyTablesMapping =
                Occupancy_Profile.getOccupancyTablesMappingsFromExcel(occupancy,Occupancy_Profile.occupancyProfileReport, 0);
        TreeMap<Integer, TreeMap<LocalTime, Integer>> trendOccupancyTablesMapping =
                Occupancy_Profile.getOccupancyTablesMappingsFromExcel(occupancy,Occupancy_Profile.occupancyProfileTrend, 0);
        util.syncOccupancyTableDataWithEnergyData(energy, "SIS Occupancy", sisOccupancyTablesMapping);
        util.syncOccupancyTableDataWithEnergyData(energy, "Trend Occupancy", trendOccupancyTablesMapping);
        util.syncOccupancyTableDataWithEnergyData(energy, "Report Occupancy", reportOccupancyTablesMapping);
    }
}
