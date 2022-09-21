import com.aspose.cells.*;
import com.sun.source.tree.Tree;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class Occupancy_Profile {
    private static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm:ss a");
    private static DateTimeFormatter timeFormatterForReport = DateTimeFormatter.ofPattern("h:mm a");
    private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a");
    private static int vavNameColInReport = 1;
    private static int vavOccupancyStateInReport = 2;
    public static int occupancyProfileReport = 15;
    public static int occupancyProfileTrend = 30;
    public static int occupancyProfileSIS = 0;
    private static int chartArrayName = 0;
    private static int chartArrayData = 1;
    private static int weeklyCategoryAxisTickLabelSpacing = 24;
    private static int weeklyCategoryAxisTickMarkSpacing = 12;
    private static int weeklyCategoryAxisTickLabelTextRotation = 90;
    private static int recordIntervalInMinute = 5;
    private static int startTimeEndTimeRowSIS = 8;
    private static int startTimeEndTimeRowTrend = 38;
    private static int startTimeEndTimeRowReport = 23;
    private static String roomName = "070-1455";
    private static String vavName = "338";

    /**
     * Used to check whether all arrays are of the same size.
     * @param arrays
     * @throws Exception if arrays are not of the same size
     */
    private static void checkParrallelArrays(List... arrays) throws Exception {
        if (arrays.length < 1){
            return;
        }
        int expectedLength = arrays[0].size();
        for (int i = 1; i < arrays.length; i++){
            if (expectedLength != arrays[i].size()){
                throw new Exception("Mismatched Array Size!");
            }
        }
    }

    /**
     * An item in this table would be: "00:00 AM" : 0.
     * The key is time and the value is 0. Will create all times in the interval of 15 mins.
     * @return an empty occupancy table
     */
    private static TreeMap<LocalTime, Integer> createEmptyOccupancyTable(){
        TreeMap<LocalTime, Integer> results = new TreeMap<>();
        LocalTime time = LocalTime.MIN;
        while (!results.containsKey(time)){
            results.put(time, 0);
            time = time.plusMinutes(recordIntervalInMinute);
        }
        return results;
    }

    /**
     * An item in this mapping would be:
     *      1: <Empty Occupancy Table>
     *
     *  where 1 denotes Monday
     * @return a mapping that has integers of day of week as key and an occupancy table as value
     */
    private static TreeMap<Integer, TreeMap<LocalTime, Integer>> createEmptyOccupancyTableMappings(){
        TreeMap<Integer, TreeMap<LocalTime, Integer>> results = new TreeMap<>();
        for (int i = 1; i <= 7; i++){
            results.put(i, createEmptyOccupancyTable());
        }
        return results;
    }

    /**
     * Main function to be called to create occupancy profiles
     * @param fileName
     */
    public static void createOccupancyProfile(String fileName){
        try{
            Workbook workbook = new Workbook(fileName);

            occupancyProfileForWebCtrlTrendData(workbook);
            occupancyProfileForSISData(workbook);
            occupancyProfileForWebCtrlReportData(workbook);

            graphOccupancyProfiles(workbook);
            workbook.save(fileName);

        } catch (Exception e){
            System.err.println("error in createOccupancyProfile " + e.getMessage());
        }
    }

    private static TreeMap<Integer, TreeMap<LocalTime, Integer>> createOccupancyProfileOf(String VAVName, String roomName,
                                                                                          ArrayList<LocalTime> reservedStartList,
                                                                                          ArrayList<LocalTime> reservedEndList,
                                                                                          ArrayList<Integer> totalEnrolledList,
                                                                                          ArrayList<String> facilityIDList,
                                                                                          ArrayList<Boolean> mondayList,
                                                                                          ArrayList<Boolean> tuesdayList,
                                                                                          ArrayList<Boolean> wednesdayList,
                                                                                          ArrayList<Boolean> thursdayList,
                                                                                          ArrayList<Boolean> fridayList,
                                                                                          ArrayList<Boolean> saturdayList,
                                                                                          ArrayList<Boolean> sundayList,
                                                                                          ArrayList<Integer> uncertaintyList,
                                                                                          ArrayList<String> VAVIDList) throws Exception {
        TreeMap<Integer, TreeMap<LocalTime, Integer>> keyDayOfWeek_valueOccupancyTable = createEmptyOccupancyTableMappings();
        int uncertainty = getMaxUncertaintyForVAV(VAVName, uncertaintyList, VAVIDList);
        ArrayList<ArrayList<Boolean>> dayList = new ArrayList<>(Arrays.asList(mondayList, tuesdayList, wednesdayList,
                thursdayList, fridayList, saturdayList, sundayList));

        for (int i = 1; i <= 7; i++){
            ArrayList<Boolean> currentList = dayList.get(i-1);
            for (int a = 0; a < currentList.size(); a++){
                if (facilityIDList.get(a).equalsIgnoreCase(roomName)) {
                    if (currentList.get(a)) {
                        TreeMap<LocalTime, Integer> occupancyTable = keyDayOfWeek_valueOccupancyTable.get(i);
                        for (LocalTime time : occupancyTable.keySet()) {
                            if ((time.isAfter(reservedStartList.get(a)) && time.isBefore(reservedEndList.get(a))) ||
                                    time.equals(reservedEndList.get(a)) ||
                                    time.equals(reservedStartList.get(a))) {
                                occupancyTable.put(time, occupancyTable.get(time) + totalEnrolledList.get(a) + uncertainty);
                            }
                        }
                    }
                }
            }
        }
        return keyDayOfWeek_valueOccupancyTable;
    }

    /**
     * Fill out the occupancy table mappings using the provided report from Webctrl
     * @param report
     * @param vavName
     * @return
     */
    private static TreeMap<Integer, TreeMap<LocalTime, Integer>> createOccupancyTableMappingsFromReport(List<String[]> report, String vavName){
        TreeMap<Integer, TreeMap<LocalTime, Integer>> keyDayOfWeek_valueOccupancyTable = createEmptyOccupancyTableMappings();
        for (String[] line: report){
            if (line.length <= vavOccupancyStateInReport || line.length <= vavOccupancyStateInReport){
                continue;
            }
            if (line[vavNameColInReport].equalsIgnoreCase(vavName)){
                String state = line[vavOccupancyStateInReport];
                parseOccupancyStateFromReport(state, keyDayOfWeek_valueOccupancyTable);
            }
        }
        return keyDayOfWeek_valueOccupancyTable;
    }

    /**
     * Obtain values from the provided columns. The column only has 2 values "Y" or "N". "Y" is True, otherwise False
     * @param worksheet
     * @param columnName
     * @return
     */
    private static ArrayList<Boolean> getBooleanFromExcel(Worksheet worksheet, String columnName){
        ArrayList<Boolean> result = new ArrayList<>();
        int initialCol = -1;
        Cells cells = worksheet.getCells();
        for (int col = 0; col <= cells.getMaxDataColumn(); col++) {
            if (cells.get(0, col).getStringValue().equalsIgnoreCase(columnName)) {
                initialCol = col;
                break;
            }
        }
        if (initialCol == -1){
            System.err.println("in getBooleanFromExcel no column with name " + columnName + " in sheet "+ worksheet.getName());
            return null;
        }
        for (int row = 1; row <= cells.getMaxDataRow(); row++){
            String val = cells.get(row, initialCol).getStringValue();
            if (val.equalsIgnoreCase("Y")){
                result.add(true);
            } else {
                result.add(false);
            }
        }
        return result;
    }

    /**
     *
     * @param worksheet
     * @param columnName
     * @return
     */
    private static ArrayList<DateTime> getDateTimeFromExcel(Worksheet worksheet, String columnName){
        ArrayList<DateTime> result = new ArrayList<>();
        int initialCol = -1;
        Cells cells = worksheet.getCells();
        for (int col = 0; col <= cells.getMaxDataColumn(); col++) {
            if (cells.get(0, col).getStringValue().equalsIgnoreCase(columnName)) {
                initialCol = col;
                break;
            }
        }
        if (initialCol == -1){
            System.err.println("in getDateTimeFromExcel no column with name " + columnName + " in sheet "+ worksheet.getName());
            return null;
        }
        for (int row = 1; row <= cells.getMaxDataRow(); row++){
            DateTime time = cells.get(row, initialCol).getDateTimeValue();
            result.add(time);
        }
        return result;
    }

    /**
     * Obtain all int values under the specified column
     * @param worksheet
     * @param columnName
     * @param scol the col to start looking
     * @param srow the row to start looking
     * @return
     */
    private static ArrayList<Integer> getIntegerfromExcel(Worksheet worksheet, String columnName, int srow, int scol){
        ArrayList<Integer> result = new ArrayList<>();
        int initialCol = -1;
        Cells cells = worksheet.getCells();
        for (int col = scol; col <= cells.getMaxDataColumn(); col++) {
            if (cells.get(srow, col).getStringValue().equalsIgnoreCase(columnName)) {
                initialCol = col;
                break;
            }
        }
        if (initialCol == -1){
            System.err.println("in getIntegerFromExcel no column with name " + columnName + " in sheet "+ worksheet.getName());
            return null;
        }
        for (int row = srow+1; row <= cells.getMaxDataRow(); row++){
            try {
                Integer number = cells.get(row, initialCol).getIntValue();
                result.add(number);
            } catch (Exception e){
                result.add(0);
            }
        }
        return result;
    }

    /**
     * This obtains all time values under the specified column
     * @param worksheet the worksheet where to look
     * @param columnName the column where the time values are
     * @return
     */
    private static ArrayList<LocalTime> getLocalTimeFromExcel(Worksheet worksheet, String columnName){
        ArrayList<LocalTime> result = new ArrayList<>();
        int initialCol = -1;
        Cells cells = worksheet.getCells();
        for (int col = 0; col <= cells.getMaxDataColumn(); col++) {
            if (cells.get(0, col).getStringValue().equalsIgnoreCase(columnName)) {
                initialCol = col;
                break;
            }
        }
        if (initialCol == -1){
            System.err.println("in getLocalTimeFromExcel no column with name " + columnName + " in sheet "+ worksheet.getName());
            return null;
        }
        for (int row = 1; row <= cells.getMaxDataRow(); row++){
            LocalTime time = LocalTime.parse(cells.get(row, initialCol).getStringValue(), timeFormatter);
            result.add(time);
        }
        return result;
    }

    private static int getMaxOccupancyNumberFor(String facilityID, ArrayList<String> facilityIDList,
                                                 ArrayList<Integer> totalEnrolledList) throws Exception {
        int result = 0;
        checkParrallelArrays(facilityIDList, totalEnrolledList);
        for (int i = 0; i < facilityIDList.size(); i++){
            if (facilityID.equalsIgnoreCase(facilityIDList.get(i))){
                if (totalEnrolledList.get(i) > result){
                    result = totalEnrolledList.get(i);
                }
            }
        }
        return result;
    }

    private static int getMaxUncertaintyForVAV(String VAVID, ArrayList<Integer> uncertaintyList,
                                                ArrayList<String> VAVIDList) throws Exception {
        checkParrallelArrays(uncertaintyList, VAVIDList);
        int maxUncertainty = 0;
        for (int i = 0; i < uncertaintyList.size(); i++){
            if (VAVID.equalsIgnoreCase(VAVIDList.get(i))){
                if (uncertaintyList.get(i) > maxUncertainty){
                    maxUncertainty = uncertaintyList.get(i);
                }
            }
        }
        return maxUncertainty;
    }

    /**
     *
     * @param worksheet
     * @param columnName
     * @param srow
     * @param scol
     * @return
     */
    private static ArrayList<String> getStringFromExcel(Worksheet worksheet, String columnName, int srow, int scol){
        ArrayList<String> result = new ArrayList<>();
        int initialCol = -1;
        Cells cells = worksheet.getCells();
        for (int col = scol; col <= cells.getMaxDataColumn(); col++) {
            if (cells.get(srow, col).getStringValue().equalsIgnoreCase(columnName)) {
                initialCol = col;
                break;
            }
        }
        if (initialCol == -1){
            System.err.println("in getStringFromExcel no column with name " + columnName + " in sheet "+ worksheet.getName());
            return null;
        }
        for (int row = srow+1; row <= cells.getMaxDataRow(); row++){
            String value = cells.get(row, initialCol).getStringValue();
            result.add(value);
        }
        return result;
    }

    private static void graphDayByDayOccupancyProfiles(Workbook workbook){
        // ********************Graph A
        // Monday
        graphOccupancyProfile(workbook, "Monday SIS vs effective schedule",
                "Population",
                new ArrayList<>(Arrays.asList("Time", "B1:CS1")),
                new ArrayList<>(Arrays.asList(
                        new ArrayList<>(Arrays.asList("SIS", "B2:CS2")),
                        new ArrayList<>(Arrays.asList("Effective Schedule", "B17:CS17"))
                )),
                0, 0, 10, 10);
        // Tuesday
        graphOccupancyProfile(workbook, "Tuesday SIS vs effective schedule",
                "Population",
                new ArrayList<>(Arrays.asList("Time", "B1:CS1")),
                new ArrayList<>(Arrays.asList(
                        new ArrayList<>(Arrays.asList("SIS", "B3:CS3")),
                        new ArrayList<>(Arrays.asList("Effective Schedule", "B18:CS18"))
                )),
                0, 10, 10, 20);
        // Wednesday
        graphOccupancyProfile(workbook, "Wednesday SIS vs effective schedule",
                "Population",
                new ArrayList<>(Arrays.asList("Time", "B1:CS1")),
                new ArrayList<>(Arrays.asList(
                        new ArrayList<>(Arrays.asList("SIS", "B4:CS4")),
                        new ArrayList<>(Arrays.asList("Effective Schedule", "B19:CS19"))
                )),
                0, 20, 10, 30);
        // Thursday
        graphOccupancyProfile(workbook, "Thursday SIS vs effective schedule",
                "Population",
                new ArrayList<>(Arrays.asList("Time", "B1:CS1")),
                new ArrayList<>(Arrays.asList(
                        new ArrayList<>(Arrays.asList("SIS", "B5:CS5")),
                        new ArrayList<>(Arrays.asList("Effective Schedule", "B20:CS20"))
                )),
                0, 30, 10, 40);
        // Friday
        graphOccupancyProfile(workbook, "Friday SIS vs effective schedule",
                "Population",
                new ArrayList<>(Arrays.asList("Time", "B1:CS1")),
                new ArrayList<>(Arrays.asList(
                        new ArrayList<>(Arrays.asList("SIS", "B6:CS6")),
                        new ArrayList<>(Arrays.asList("Effective Schedule", "B21:CS21"))
                )),
                0, 40, 10, 50);
        // Saturday
        graphOccupancyProfile(workbook, "Saturday SIS vs effective schedule",
                "Population",
                new ArrayList<>(Arrays.asList("Time", "B1:CS1")),
                new ArrayList<>(Arrays.asList(
                        new ArrayList<>(Arrays.asList("SIS", "B7:CS7")),
                        new ArrayList<>(Arrays.asList("Effective Schedule", "B22:CS22"))
                )),
                0, 50, 10, 60);
        // Sunday
        graphOccupancyProfile(workbook, "Sunday SIS vs effective schedule",
                "Population",
                new ArrayList<>(Arrays.asList("Time", "B1:CS1")),
                new ArrayList<>(Arrays.asList(
                        new ArrayList<>(Arrays.asList("SIS", "B8:CS8")),
                        new ArrayList<>(Arrays.asList("Effective Schedule", "B23:CS23"))
                )),
                0, 60, 10, 70);

        // ********************Graph A
        // Monday
        graphOccupancyProfile(workbook, "Monday occupancy sensor vs effective schedule",
                "Population",
                new ArrayList<>(Arrays.asList("Time", "B1:CS1")),
                new ArrayList<>(Arrays.asList(
                        new ArrayList<>(Arrays.asList("occupancy sensor", "B32:CS32")),
                        new ArrayList<>(Arrays.asList("Effective Schedule", "B17:CS17"))
                )),
                10, 0, 20,  10);
        // Tuesday
        graphOccupancyProfile(workbook, "Tuesday occupancy sensor vs effective schedule",
                "Population",
                new ArrayList<>(Arrays.asList("Time", "B1:CS1")),
                new ArrayList<>(Arrays.asList(
                        new ArrayList<>(Arrays.asList("occupancy sensor", "B33:CS33")),
                        new ArrayList<>(Arrays.asList("Effective Schedule", "B18:CS18"))
                )),
                10, 10, 20, 20);
        // Wednesday
        graphOccupancyProfile(workbook, "Wednesday occupancy sensor vs effective schedule",
                "Population",
                new ArrayList<>(Arrays.asList("Time", "B1:CS1")),
                new ArrayList<>(Arrays.asList(
                        new ArrayList<>(Arrays.asList("occupancy sensor", "B34:CS34")),
                        new ArrayList<>(Arrays.asList("Effective Schedule", "B19:CS19"))
                )),
                10, 20, 20, 30);
        // Thursday
        graphOccupancyProfile(workbook, "Thursday occupancy sensor vs effective schedule",
                "Population",
                new ArrayList<>(Arrays.asList("Time", "B1:CS1")),
                new ArrayList<>(Arrays.asList(
                        new ArrayList<>(Arrays.asList("occupancy sensor", "B35:CS35")),
                        new ArrayList<>(Arrays.asList("Effective Schedule", "B20:CS20"))
                )),
                10, 30, 20, 40);
        // Friday
        graphOccupancyProfile(workbook, "Friday occupancy sensor vs effective schedule",
                "Population",
                new ArrayList<>(Arrays.asList("Time", "B1:CS1")),
                new ArrayList<>(Arrays.asList(
                        new ArrayList<>(Arrays.asList("occupancy sensor", "B36:CS36")),
                        new ArrayList<>(Arrays.asList("Effective Schedule", "B21:CS21"))
                )),
                10, 40, 20, 50);
        // Saturday
        graphOccupancyProfile(workbook, "Saturday occupancy sensor vs effective schedule",
                "Population",
                new ArrayList<>(Arrays.asList("Time", "B1:CS1")),
                new ArrayList<>(Arrays.asList(
                        new ArrayList<>(Arrays.asList("occupancy sensor", "B37:CS37")),
                        new ArrayList<>(Arrays.asList("Effective Schedule", "B22:CS22"))
                )),
                10, 50, 20, 60);
        // Sunday
        graphOccupancyProfile(workbook, "Sunday occupancy sensor vs effective schedule",
                "Population",
                new ArrayList<>(Arrays.asList("Time", "B1:CS1")),
                new ArrayList<>(Arrays.asList(
                        new ArrayList<>(Arrays.asList("occupancy sensor", "B38:CS38")),
                        new ArrayList<>(Arrays.asList("Effective Schedule", "B23:CS23"))
                )),
                10, 60, 20, 70);
    }

    private static void graphOccupancyProfile(Workbook workbook,
                                               String graphName,
                                               String valueAxisName,
                                               ArrayList<String> categoryValue,
                                               ArrayList<ArrayList<String>> nSeriesValues,
                                               int upperLeftRow, int upperLeftColumn,
                                               int lowerRightRow, int lowerRightColumn){
        try {
            Worksheet worksheet = util.getWorksheetFromWorkbook(workbook,"OccupancyProfile");
            int chartIndex = worksheet.getCharts().add(ChartType.LINE, upperLeftRow, upperLeftColumn, lowerRightRow, lowerRightColumn);
            Chart chart = worksheet.getCharts().get(chartIndex);
            chart.getTitle().setText(graphName);
            chart.getValueAxis().getTitle().setText(valueAxisName);
            chart.getCategoryAxis().getTitle().setText(categoryValue.get(chartArrayName));
            chart.getCategoryAxis().getTickLabels().setNumberFormat("h:mm");
            for (int i = 0; i < nSeriesValues.size(); i++){
                ArrayList<String> currentNSeries = nSeriesValues.get(i);
                chart.getNSeries().add(currentNSeries.get(chartArrayData), false);
                chart.getNSeries().get(i).setName(currentNSeries.get(chartArrayName));
            }
            chart.getNSeries().setCategoryData(categoryValue.get(chartArrayData));
        } catch (Exception e){
            System.err.println("Errors in graphOccupancyProfiles " + e.getMessage());
        }
    }

    private static void graphOccupancyProfiles(Workbook workbook){
        // graphDayByDayOccupancyProfiles(workbook);
        Worksheet worksheet = util.getWorksheetFromWorkbook(workbook,"OccupancyProfile");
        Worksheet weeklyWorksheet = util.getWorksheetFromWorkbook(workbook, "ContinuousOccupancyProfile");
        Worksheet hoursWorksheet = util.getWorksheetFromWorkbook(workbook, "hoursOccupancyProfile");

        // readGraph(weeklyWorksheet);
        graphWeekly(weeklyWorksheet, "OccupancyProfile",
                "Population",
                new ArrayList<>(Arrays.asList("Time", "B1:YW1")),
                new ArrayList<>(Arrays.asList(
                        new ArrayList<>(Arrays.asList("Occupancy Sensor", "B32:YW32")),
                        new ArrayList<>(Arrays.asList("SIS Data", "B2:YW2")),
                        new ArrayList<>(Arrays.asList("Effective Schedule", "B17:YW17"))
                        )),
                new ArrayList<>(Arrays.asList(
                        ChartType.LINE,
                        ChartType.LINE,
                        ChartType.LINE
                )),
                new ArrayList<>(Arrays.asList(
                        Color.getOrange(),
                        Color.getBlue(),
                        Color.getGray()
                )),
                new ArrayList<>(Arrays.asList("Occupancy Sensor State")),
                new ArrayList<>(Arrays.asList(true, false, false)),
                0, 0, 20, 20);

        graphHours(hoursWorksheet, "Room Hours Use per Day", "",
                new ArrayList<>(Arrays.asList("", "A1:G1")),
                new ArrayList<>(Arrays.asList(
                        new ArrayList<>(Arrays.asList("Actual Space Use", "A32:G32")),
                        new ArrayList<>(Arrays.asList("Event Schedule", "A2:G2")),
                        new ArrayList<>(Arrays.asList("Equipment Schedule", "A17:G17"))
                )),
                new ArrayList<>(Arrays.asList(
                        Color.getOrange(),
                        Color.getBlue(),
                        Color.getGray()
                )),
                0, 0, 20, 20
                );
        graphHours(hoursWorksheet, "Room Hours Use per Week", "",
                new ArrayList<>(Arrays.asList("", "H1")),
                new ArrayList<>(Arrays.asList(
                        new ArrayList<>(Arrays.asList("Actual Space Use", "H32")),
                        new ArrayList<>(Arrays.asList("Event Schedule", "H2")),
                        new ArrayList<>(Arrays.asList("Equipment Schedule", "H17"))
                )),
                new ArrayList<>(Arrays.asList(
                        Color.getOrange(),
                        Color.getBlue(),
                        Color.getGray()
                )),
                20, 0, 40, 20
        );
    }

    private static void graphHours(Worksheet worksheet,
                                    String graphName,
                                    String valueAxisName,
                                    ArrayList<String> categoryValue,
                                    ArrayList<ArrayList<String>> nSeriesValues,
                                    ArrayList<Color> colorsList,
                                    int upperLeftRow, int upperLeftColumn,
                                    int lowerRightRow, int lowerRightColumn){
        try {
            int chartIndex = worksheet.getCharts().add(ChartType.COLUMN, upperLeftRow, upperLeftColumn, lowerRightRow, lowerRightColumn);
            checkParrallelArrays(nSeriesValues, colorsList);
            Chart chart = worksheet.getCharts().get(chartIndex);
            chart.getTitle().setText(graphName);
            chart.getPlotArea().getArea().setBackgroundColor(Color.getWhite());
            chart.getPlotArea().getArea().setForegroundColor(Color.getWhite());
            chart.getValueAxis().getTitle().setText(valueAxisName);

            chart.getCategoryAxis().getTitle().setText(categoryValue.get(chartArrayName));
            for (int i = 0; i < nSeriesValues.size(); i++){
                ArrayList<String> currentNSeries = nSeriesValues.get(i);
                chart.getNSeries().add(currentNSeries.get(chartArrayData), false);
                chart.getNSeries().get(i).setName(currentNSeries.get(chartArrayName));
                chart.getNSeries().get(i).getArea().setForegroundColor(colorsList.get(i));
            }
            chart.getNSeries().setCategoryData(categoryValue.get(chartArrayData));
        } catch (Exception e){
            System.err.println("Errors in graphHours " + e.getMessage());
        }
    }


    private static void graphWeekly(Worksheet worksheet,
                                      String graphName,
                                      String valueAxisName,
                                      ArrayList<String> categoryValue,
                                      ArrayList<ArrayList<String>> nSeriesValues,
                                      ArrayList<Integer> chartTypes,
                                      ArrayList<Color> colorsList,
                                      ArrayList<String> secondaryAxisValue,
                                      ArrayList<Boolean> onSecondaryAxisList,
                                      int upperLeftRow, int upperLeftColumn,
                                      int lowerRightRow, int lowerRightColumn){
        try {
            int chartIndex = worksheet.getCharts().add(ChartType.LINE, upperLeftRow, upperLeftColumn, lowerRightRow, lowerRightColumn);
            checkParrallelArrays(nSeriesValues, chartTypes, colorsList, onSecondaryAxisList);
            Chart chart = worksheet.getCharts().get(chartIndex);
            chart.getTitle().setText(graphName);
            chart.getPlotArea().getArea().setBackgroundColor(Color.getWhite());
            chart.getPlotArea().getArea().setForegroundColor(Color.getWhite());
            chart.getValueAxis().getTitle().setText(valueAxisName);

            chart.getCategoryAxis().getTitle().setText(categoryValue.get(chartArrayName));
            for (int i = 0; i < nSeriesValues.size(); i++){
                ArrayList<String> currentNSeries = nSeriesValues.get(i);
                chart.getNSeries().add(currentNSeries.get(chartArrayData), false);
                chart.getNSeries().get(i).setName(currentNSeries.get(chartArrayName));
                chart.getNSeries().get(i).setType(chartTypes.get(i));
                chart.getNSeries().get(i).getBorder().setColor(colorsList.get(i));
            }
            chart.getNSeries().setCategoryData(categoryValue.get(chartArrayData));

            // Can only set Secondary axis after setting category axis data
            for (int i = 0; i < nSeriesValues.size(); i++){
                chart.getNSeries().get(i).setPlotOnSecondAxis(onSecondaryAxisList.get(i));
            }

            // Formatting Category Axis
            chart.getCategoryAxis().setTickLabelSpacing(weeklyCategoryAxisTickLabelSpacing);
            chart.getCategoryAxis().setTickMarkSpacing(weeklyCategoryAxisTickMarkSpacing);
            chart.getCategoryAxis().getMajorGridLines().setVisible(false);
            chart.getCategoryAxis().getTickLabels().setRotationAngle(weeklyCategoryAxisTickLabelTextRotation);

            // Formatting Secondary ValueAxis
            chart.getSecondValueAxis().setVisible(true);
            chart.getSecondValueAxis().getTitle().setText(secondaryAxisValue.get(chartArrayName));

            chart.getValueAxis().getMajorGridLines().setVisible(false);
        } catch (Exception e){
            System.err.println("Errors in graphOccupancyProfiles " + e.getMessage());
        }
    }

    public static void main(String[] args){
        String filename = System.getProperty("user.dir") + "/src/OCCUPANCY.xlsx";
        createOccupancyProfile(filename);
    }

    private static void multiplyOccupiedStateByMaxOccupancyValue(TreeMap<Integer, TreeMap<LocalTime, Integer>> keyDayOfWeek_valueOccupancyTable, Workbook workbook) throws Exception {
        Worksheet sisDataWorksheet = util.getWorksheetFromWorkbook(workbook, "SIS data");
        Worksheet roomInputDataWorksheet = util.getWorksheetFromWorkbook(workbook, "Room Input data0");
        ArrayList<Integer> totalEnrolledList = getIntegerfromExcel(sisDataWorksheet, "Tot Enrl", 0, 0);
        ArrayList<String> facilityIDList = getStringFromExcel(sisDataWorksheet, "Facil ID", 0, 0);
        ArrayList<Integer> uncertaintyList = getIntegerfromExcel(roomInputDataWorksheet, "Uncertainty", 11, 0);
        ArrayList<String> vavIDList = getStringFromExcel(roomInputDataWorksheet, "VAV_ID", 11, 0);

        int uncertainty = getMaxUncertaintyForVAV(vavName, uncertaintyList, vavIDList);
        int totalEnrolled = getMaxOccupancyNumberFor(roomName, facilityIDList, totalEnrolledList);

        for (TreeMap<LocalTime, Integer> occupancyTable : keyDayOfWeek_valueOccupancyTable.values()){
            for (LocalTime localTime: occupancyTable.keySet()){
                int value = occupancyTable.get(localTime);
                if (value == 1){
                    occupancyTable.put(localTime, uncertainty + totalEnrolled);
                }
            }
        }
    }

    private static void occupancyProfileForSISData(Workbook workbook) throws Exception {
        Worksheet worksheet = util.getWorksheetFromWorkbook(workbook,"SIS data");
        Worksheet roomInputWorksheet = util.getWorksheetFromWorkbook(workbook, "Room Input data0");
        Worksheet occupancyProfileWorksheet = util.getWorksheetFromWorkbook(workbook,"OccupancyProfile");
        Worksheet continuousOccupancyProfileWorksheet = util.getWorksheetFromWorkbook(workbook, "ContinuousOccupancyProfile");
        Worksheet hoursOccupancyProfileWorksheet = util.getWorksheetFromWorkbook(workbook, "hoursOccupancyProfile");
        Worksheet statusesOccupancyProfileWorksheet = util.getWorksheetFromWorkbook(workbook, "3statuses");



        ArrayList<LocalTime> reservedStartList = getLocalTimeFromExcel(worksheet, "Mtg Start");
        ArrayList<LocalTime> reservedEndList = getLocalTimeFromExcel(worksheet, "Mtg End");
        ArrayList<Integer> totalEnrolledList = getIntegerfromExcel(worksheet, "Tot Enrl", 0, 0);
        ArrayList<String> facilityIDList = getStringFromExcel(worksheet, "Facil ID", 0, 0);
        ArrayList<Boolean> mondayList = getBooleanFromExcel(worksheet, "Mon");
        ArrayList<Boolean> tuesdayList = getBooleanFromExcel(worksheet, "Tues");
        ArrayList<Boolean> wednesdayList = getBooleanFromExcel(worksheet, "Wed");
        ArrayList<Boolean> thursdayList = getBooleanFromExcel(worksheet, "Thurs");
        ArrayList<Boolean> fridayList = getBooleanFromExcel(worksheet, "Fri");
        ArrayList<Boolean> saturdayList = getBooleanFromExcel(worksheet, "Sat");
        ArrayList<Boolean> sundayList = getBooleanFromExcel(worksheet, "Sun");
        ArrayList<Integer> uncertaintyList = getIntegerfromExcel(roomInputWorksheet, "Uncertainty", 11, 0);
        ArrayList<String> roomIDList = getStringFromExcel(roomInputWorksheet, "RoomID", 11, 0);
        ArrayList<String> buildingIDList = getStringFromExcel(roomInputWorksheet, "BuildingID", 11, 0);
        ArrayList<String> vavIDList = getStringFromExcel(roomInputWorksheet, "VAV_ID", 11, 0);

        /// MAKING SURE ALL LIST ARE OF THE SAME LENGTH
        checkParrallelArrays(reservedEndList, reservedStartList, totalEnrolledList, facilityIDList,
                mondayList, thursdayList, tuesdayList, wednesdayList, fridayList, saturdayList, sundayList);
        checkParrallelArrays(uncertaintyList, roomIDList, buildingIDList, vavIDList);

        TreeMap<Integer, TreeMap<LocalTime, Integer>> keyDayOfWeek_valueOccupancyTable = createOccupancyProfileOf(vavName, roomName,
                reservedStartList, reservedEndList, totalEnrolledList, facilityIDList, mondayList, tuesdayList, wednesdayList,
                thursdayList, fridayList, saturdayList, sundayList, uncertaintyList,vavIDList);

        writeOccupancyTablesMappingstoExcel(occupancyProfileWorksheet, keyDayOfWeek_valueOccupancyTable, occupancyProfileSIS, 0);
        writeOccupancyTableMappingsContinuouslyToExcel(continuousOccupancyProfileWorksheet, keyDayOfWeek_valueOccupancyTable, occupancyProfileSIS, 0);
        writeOccupancyTableMappingsAsPeriodsToExcel(hoursOccupancyProfileWorksheet, keyDayOfWeek_valueOccupancyTable, occupancyProfileSIS, 0);

        TreeMap<Integer, TreeMap<LocalTime, Integer>> keyDayOfWeek_valueOccupancyTableWith3Statuses = cloneOccupancyProfileMappings(keyDayOfWeek_valueOccupancyTable);
        updateOccupancyProfileMappingsWith3Statuses(keyDayOfWeek_valueOccupancyTableWith3Statuses);
        writeOccupancyTablesMappingstoExcel(statusesOccupancyProfileWorksheet, keyDayOfWeek_valueOccupancyTableWith3Statuses, occupancyProfileSIS, 0);
        writeStartTimeEndTimeToExcel(statusesOccupancyProfileWorksheet, getStartTime(keyDayOfWeek_valueOccupancyTable), getEndTime(keyDayOfWeek_valueOccupancyTable), startTimeEndTimeRowSIS, 0);
    }

    private static void occupancyProfileForWebCtrlReportData(Workbook workbook) throws Exception {
        String vav = "70-VAV-338~";
        Worksheet inputWorksheet = util.getWorksheetFromWorkbook(workbook,"WebCtrlReportInput");
        Worksheet rawDataWorksheet = util.getWorksheetFromWorkbook(workbook,"ReportRawData");
        Worksheet occupancyProfileWorksheet = util.getWorksheetFromWorkbook(workbook,"OccupancyProfile");
        Worksheet continuousOccupancyProfileWorksheet = util.getWorksheetFromWorkbook(workbook, "ContinuousOccupancyProfile");
        Worksheet hoursOccupancyProfileWorksheet = util.getWorksheetFromWorkbook(workbook, "hoursOccupancyProfile");
        Worksheet statusesOccupancyProfileWorksheet = util.getWorksheetFromWorkbook(workbook, "3statuses");

        List<String[]> report = util.pullReportsFromWebCtrl(inputWorksheet, "Effective Schedule");
        util.saveReportToExcel(rawDataWorksheet, report);
        TreeMap<Integer, TreeMap<LocalTime, Integer>> keyDayOfWeek_valueOccupancyTable = createOccupancyTableMappingsFromReport(report, vav);

        multiplyOccupiedStateByMaxOccupancyValue(keyDayOfWeek_valueOccupancyTable, workbook);

        writeOccupancyTablesMappingstoExcel(occupancyProfileWorksheet, keyDayOfWeek_valueOccupancyTable, occupancyProfileReport, 0);
        writeOccupancyTableMappingsContinuouslyToExcel(continuousOccupancyProfileWorksheet, keyDayOfWeek_valueOccupancyTable, occupancyProfileReport, 0);
        writeOccupancyTableMappingsAsPeriodsToExcel(hoursOccupancyProfileWorksheet, keyDayOfWeek_valueOccupancyTable, occupancyProfileReport, 0);


        TreeMap<Integer, TreeMap<LocalTime, Integer>> keyDayOfWeek_valueOccupancyTableWith3Statuses = cloneOccupancyProfileMappings(keyDayOfWeek_valueOccupancyTable);
        updateOccupancyProfileMappingsWith3Statuses(keyDayOfWeek_valueOccupancyTableWith3Statuses);
        writeOccupancyTablesMappingstoExcel(statusesOccupancyProfileWorksheet, keyDayOfWeek_valueOccupancyTableWith3Statuses, occupancyProfileReport, 0);
        writeStartTimeEndTimeToExcel(statusesOccupancyProfileWorksheet, getStartTime(keyDayOfWeek_valueOccupancyTable), getEndTime(keyDayOfWeek_valueOccupancyTable), startTimeEndTimeRowReport, 0);
    }

    private static void occupancyProfileForWebCtrlTrendData(Workbook workbook) throws Exception {


        Worksheet inputWorksheet = util.getWorksheetFromWorkbook(workbook,"WebCtrl Input");
        Worksheet rawDataWorksheet = util.getWorksheetFromWorkbook(workbook,"Trend Data");
        Worksheet occupancyProfileWorksheet = util.getWorksheetFromWorkbook(workbook,"OccupancyProfile");
        Worksheet continuousOccupancyProfileWorksheet = util.getWorksheetFromWorkbook(workbook, "ContinuousOccupancyProfile");
        Worksheet hoursOccupancyProfileWorksheet = util.getWorksheetFromWorkbook(workbook, "hoursOccupancyProfile");
        Worksheet statusesOccupancyProfileWorksheet = util.getWorksheetFromWorkbook(workbook, "3statuses");

        String[] results = util.pullDataFromWebCtrl(inputWorksheet, "Occupancy Contact State");
        util.saveRawDataToExcel("Occupancy Contact State", rawDataWorksheet, 0, 0, results);
        TreeMap<Integer, TreeMap<LocalTime, Integer>> keyDayOfWeek_valueOccupancyTable = createEmptyOccupancyTableMappings();

        for (int i = 0; i < results.length; i+=2){
            LocalDateTime date = LocalDateTime.parse(results[i], dateTimeFormatter);
            Boolean isOn = results[i + 1].equalsIgnoreCase("1");

            int dayOfWeek = date.getDayOfWeek().getValue();
            TreeMap<LocalTime, Integer> occupancyTable = keyDayOfWeek_valueOccupancyTable.get(dayOfWeek);
            LocalTime time = date.toLocalTime();
            ArrayList<LocalTime> timeBoxes = new ArrayList<>(occupancyTable.keySet());
            Collections.sort(timeBoxes);
            for (LocalTime timeBox: timeBoxes){
                if (timeBox.isAfter(time) || timeBox.equals(time)) {
                    if (isOn) {
                        occupancyTable.put(timeBox, 1);
                    } else {
                        occupancyTable.put(timeBox, 0);
                    }
                }
            }
        }
        writeOccupancyTablesMappingstoExcel(occupancyProfileWorksheet, keyDayOfWeek_valueOccupancyTable, occupancyProfileTrend, 0);
        writeOccupancyTableMappingsContinuouslyToExcel(continuousOccupancyProfileWorksheet, keyDayOfWeek_valueOccupancyTable, occupancyProfileTrend, 0);
        writeOccupancyTableMappingsAsPeriodsToExcel(hoursOccupancyProfileWorksheet, keyDayOfWeek_valueOccupancyTable, occupancyProfileTrend, 0);

        TreeMap<Integer, TreeMap<LocalTime, Integer>> keyDayOfWeek_valueOccupancyTableWith3Statuses = cloneOccupancyProfileMappings(keyDayOfWeek_valueOccupancyTable);
        updateOccupancyProfileMappingsWith3Statuses(keyDayOfWeek_valueOccupancyTableWith3Statuses);
        writeOccupancyTablesMappingstoExcel(statusesOccupancyProfileWorksheet, keyDayOfWeek_valueOccupancyTableWith3Statuses, occupancyProfileTrend, 0);
        writeStartTimeEndTimeToExcel(statusesOccupancyProfileWorksheet, getStartTime(keyDayOfWeek_valueOccupancyTable), getEndTime(keyDayOfWeek_valueOccupancyTable), startTimeEndTimeRowTrend, 0);
    }

    private static void parseOccupancyStateFromReport(String state, TreeMap<Integer, TreeMap<LocalTime, Integer>> keyDayOfWeek_valueOccupancyTable){
        String[] states = state.split("\n");
        List<String> occupiedStates = new ArrayList<>();
        List<String> unoccupiedStates = new ArrayList<>();
        for (String istate: states){
            String status = istate.split(" ")[0];
            if (status.equalsIgnoreCase("occupied")){
                occupiedStates.add(istate);
            } else{
                unoccupiedStates.add(istate);
            }
        }
        for (String occupiedState: occupiedStates){
            String[] parts1 = occupiedState.split("from");
            String[] parts = parts1[1].split("to");
            LocalTime startTime = LocalTime.parse(parts[0].strip(), timeFormatterForReport);
            LocalTime endTime = LocalTime.parse(parts[1].strip(), timeFormatterForReport);
            for (TreeMap<LocalTime, Integer> occupancyProfile : keyDayOfWeek_valueOccupancyTable.values()){
                for (LocalTime time : occupancyProfile.keySet()){
                    if (time.equals(startTime) || time.equals(endTime) ||
                            (time.isAfter(startTime) && time.isBefore(endTime)) || startTime.equals(endTime)){
                        occupancyProfile.put(time, 1);
                    }
                }
            }
        }
    }

    private static void readGraph(Worksheet worksheet){
        Chart chart = worksheet.getCharts().get(0);
    }

    private static void writeOccupancyTableMappingsContinuouslyToExcel(Worksheet worksheet, TreeMap<Integer, TreeMap<LocalTime, Integer>> keyDayOfWeek_valueOccupancyTable, int row, int col){
        Cells cells = worksheet.getCells();
        cells.get(row, col).setValue("Time");
        cells.get(row+1, col).setValue("Value");
        col++;
        for (Integer dayOfWeek : keyDayOfWeek_valueOccupancyTable.keySet()){
            String code = DayOfWeek.of(dayOfWeek).toString().substring(0,3);
            TreeMap<LocalTime, Integer> occupancyTable = keyDayOfWeek_valueOccupancyTable.get(dayOfWeek);
            for (LocalTime time: occupancyTable.keySet()){
                cells.get(row, col).setValue(code + " " + time.toString());
                cells.get(row+1, col).setValue(occupancyTable.get(time));
                col++;
            }
        }
    }

    /**
     * This will create the following table in Excel:
     *          | Monday | Tuesday | .....
     *  Schedule| 20     | 30      | .....
     *  SIS data| 30     | 20      | .....
     *  where 20 and 30 are examples of room hours used in that day
     * @param worksheet
     * @param keyDayOfWeek_valueOccupancyTable
     * @param row
     * @param col
     */
    private static void writeOccupancyTableMappingsAsPeriodsToExcel(Worksheet worksheet, TreeMap<Integer, TreeMap<LocalTime, Integer>> keyDayOfWeek_valueOccupancyTable, int row, int col) throws Exception {
        Cells cells = worksheet.getCells();
        double allTimeTotalHours = 0.0;
        for (Integer dayOfWeek: keyDayOfWeek_valueOccupancyTable.keySet()){
            String code = DayOfWeek.of(dayOfWeek).toString();
            cells.get(row, col).setValue(code);
            TreeMap<LocalTime, Integer> occupancyTable = keyDayOfWeek_valueOccupancyTable.get(dayOfWeek);
            long totalMinutes = 0;
            if (occupancyTable.keySet().size() < 1){
                throw new Exception("Not enough time to use writeOccupancyTableMappingsAsPeriodsToExcel");
            }
            LocalTime[] localTimes = occupancyTable.keySet().toArray(new LocalTime[0]);
            LocalTime initialLocalTime = localTimes[0];
            LocalTime previousLocalTime = null;
            for (LocalTime time: occupancyTable.keySet()){
                if (previousLocalTime == null){
                    if (occupancyTable.get(time) > 0){
                        previousLocalTime = time;
                    }
                } else {
                    if (occupancyTable.get(time) == 0){
                        long minutes = previousLocalTime.until(time, ChronoUnit.MINUTES) - recordIntervalInMinute;
                        totalMinutes += minutes;
                        previousLocalTime = null;
                    }
                }
            }
            double totalHours = 0;
            if (previousLocalTime != null){
                if (previousLocalTime.equals(initialLocalTime)){
                     totalHours = 24;
                } else {
                    totalMinutes += previousLocalTime.until(LocalTime.MIDNIGHT, ChronoUnit.MINUTES) - recordIntervalInMinute;
                    totalHours = (double) totalMinutes/ 60.0;
                }
            } else {
                totalHours = (double) totalMinutes / 60.0;
            }
            cells.get(row+1, col).setValue(totalHours);
            col++;
            allTimeTotalHours += totalHours;
        }
        cells.get(row, col).setValue("Total");
        cells.get(row+1, col).setValue(allTimeTotalHours);
    }

    /**
     * Deep copy the provided occupancy profile mapping
     * @param keyDayOfWeek_valueOccupancyTable
     * @return
     */
    private static TreeMap<Integer, TreeMap<LocalTime, Integer>> cloneOccupancyProfileMappings(TreeMap<Integer, TreeMap<LocalTime, Integer>>  keyDayOfWeek_valueOccupancyTable){
        TreeMap<Integer, TreeMap<LocalTime, Integer>> result = new TreeMap<>();
        for (Integer dayOfWeek : keyDayOfWeek_valueOccupancyTable.keySet()){
            TreeMap<LocalTime, Integer> newList = new TreeMap<>();
            TreeMap<LocalTime, Integer> currentList = keyDayOfWeek_valueOccupancyTable.get(dayOfWeek);

            for (LocalTime time: currentList.keySet()){
                Integer currentOccupancy = currentList.get(time);
                newList.put(time, currentOccupancy);
            }
            result.put(dayOfWeek, newList);
        }
        return result;
    }

    /**
     * Update the values in the mapping with 3 new statuses:
     * 1) Occupied (denoted by any value > 0)
     * 2) NonOccupied (denoted by 0)
     * 3) Standby (denoted by -1)
     * @param keyDayOfWeek_valueOccupancyTable
     */
    private static void updateOccupancyProfileMappingsWith3Statuses(TreeMap<Integer, TreeMap<LocalTime, Integer>>  keyDayOfWeek_valueOccupancyTable) throws Exception {
        // Figuring out standby time
        for (Integer dayOfWeek : keyDayOfWeek_valueOccupancyTable.keySet()){
            // figuring out the first occupied time
            TreeMap<LocalTime, Integer> currentOccupancyTable = keyDayOfWeek_valueOccupancyTable.get(dayOfWeek);
            updateOccupancyProfileWith3Statuses(currentOccupancyTable);
        }
    }

    /**
     * Update the values in the profile with 3 new statuses:
     * 1) Occupied (denoted by any value > 0)
     * 2) NonOccupied (denoted by 0)
     * 3) Standby (denoted by -1)
     * Assuming that the recordIntervalInMinute will loop back to 00:00 from 00:00 (a factor of 60)
     * @param occupancyTable
     */
    private static void updateOccupancyProfileWith3Statuses(TreeMap<LocalTime, Integer> occupancyTable) throws Exception {
        LocalTime start = null;
        ArrayList<LocalTime> startTimes = new ArrayList<>();
        ArrayList<LocalTime> endTimes = new ArrayList<>();
        for (LocalTime time: occupancyTable.keySet()){
            if (start == null){
                if (occupancyTable.get(time) == 0){
                    start = time;
                }
            } else {
                if (occupancyTable.get(time) > 0){
                    startTimes.add(start);
                    endTimes.add(time);
                    start = null;
                }
            }
        }
        checkParrallelArrays(startTimes, endTimes);
        for (int i = 0; i < startTimes.size(); i++){
            if (!startTimes.get(i).equals(LocalTime.MIN)) {
                updateStandby(occupancyTable, startTimes.get(i), endTimes.get(i));
            }
        }
    }

    /**
     * Update values from start time to end time with -1 (end inclusive)
     * @param occupancyTable
     * @param startTime
     * @param endTime
     */
    private static void updateStandby(TreeMap<LocalTime, Integer> occupancyTable, LocalTime startTime, LocalTime endTime){
        LocalTime currentTime = startTime;
        while (!currentTime.equals(endTime)){
            occupancyTable.put(currentTime, -1);
            currentTime = currentTime.plusMinutes(recordIntervalInMinute);
        }
    }

    private static TreeMap<Integer, LocalTime> getStartTime(TreeMap<Integer, TreeMap<LocalTime, Integer>>  keyDayOfWeek_valueOccupancyTable){
        TreeMap<Integer, LocalTime> result = new TreeMap<>();
        for (Integer dayOfWeek: keyDayOfWeek_valueOccupancyTable.keySet()){
            LocalTime start = LocalTime.MIN;
            TreeMap<LocalTime, Integer> occupancyTable = keyDayOfWeek_valueOccupancyTable.get(dayOfWeek);
            for (LocalTime time: occupancyTable.keySet()){
                if (occupancyTable.get(time) == 0){
                    start = time;
                } else {
                    start = time;
                    break;
                }
            }
            if (start == Collections.max(occupancyTable.keySet())){
                start = LocalTime.MIN;
            }
            result.put(dayOfWeek, start);
        }
        return result;
    }

    private static TreeMap<Integer, LocalTime> getEndTime(TreeMap<Integer, TreeMap<LocalTime, Integer>>  keyDayOfWeek_valueOccupancyTable){
        TreeMap<Integer, LocalTime> result = new TreeMap<>();
        for (Integer dayOfWeek: keyDayOfWeek_valueOccupancyTable.keySet()){
            LocalTime end = LocalTime.MIN;
            TreeMap<LocalTime, Integer> occupancyTable = keyDayOfWeek_valueOccupancyTable.get(dayOfWeek);
            for (LocalTime time: occupancyTable.keySet()){
                if (occupancyTable.get(time) != 0){
                    end = time;
                }
            }
            if (end == Collections.max(occupancyTable.keySet())){
                end = LocalTime.of(LocalTime.MAX.getHour(),LocalTime.MAX.getMinute());
            }

            result.put(dayOfWeek, end);
        }
        return result;
    }

    private static void writeStartTimeEndTimeToExcel(Worksheet worksheet, TreeMap<Integer, LocalTime> startTimes, TreeMap<Integer, LocalTime> endTimes, int srow, int scol){
        Cells cells = worksheet.getCells();
        cells.get(srow, scol).setValue("Date");
        cells.get(srow+1, scol).setValue("StartTime");
        cells.get(srow+2, scol).setValue("EndTime");
        for (Integer dayOfWeek: startTimes.keySet()){
            cells.get(srow, scol+dayOfWeek).setValue(DayOfWeek.of(dayOfWeek));
            cells.get(srow+1, scol+dayOfWeek).setValue(startTimes.get(dayOfWeek).toString());
        }
        for (Integer dayOfWeek: endTimes.keySet()){
            cells.get(srow+2, scol+dayOfWeek).setValue(endTimes.get(dayOfWeek).toString());
        }
    }

    private static void writeOccupancyTablesMappingstoExcel(Worksheet worksheet, TreeMap<Integer, TreeMap<LocalTime, Integer>> keyDayOfWeek_valueOccupancyTable, int row, int col){
        Cells cells = worksheet.getCells();
        int intialCol = col;
        for (Integer dayOfWeek : keyDayOfWeek_valueOccupancyTable.keySet()){
            cells.get(row, col).setValue("Time");
            cells.get(row+dayOfWeek, col).setValue(DayOfWeek.of(dayOfWeek));
            col++;
            TreeMap<LocalTime, Integer> occupancyTable = keyDayOfWeek_valueOccupancyTable.get(dayOfWeek);
            for (LocalTime time: occupancyTable.keySet()){
                cells.get(row, col).setValue(time.toString());
                cells.get(row+dayOfWeek, col).setValue(occupancyTable.get(time));
                col++;
            }
            col=intialCol;
        }
    }

    public static TreeMap<Integer, TreeMap<LocalTime, Integer>> getOccupancyTablesMappingsFromExcel(Worksheet worksheet, int srow, int scol){
        TreeMap<Integer, TreeMap<LocalTime, Integer>> result = new TreeMap<>();
        Cells cells = worksheet.getCells();
        for (int i = 1; i <= 7; i++){
            TreeMap<LocalTime, Integer> timeIntegerTreeMap = new TreeMap<>();
            result.put(i, timeIntegerTreeMap);

            for (int a = 1; a <= cells.getMaxDataColumn(); a++){
                LocalTime currentTime = LocalTime.parse(cells.get(srow, scol+a).getStringValue());
                Integer occupancyValue = cells.get(srow+i, scol+a).getIntValue();
                timeIntegerTreeMap.put(currentTime, occupancyValue);
            }
        }
        return result;
    }
}
