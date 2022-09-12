import com.aspose.cells.Cells;
import com.aspose.cells.DateTime;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.sun.source.tree.Tree;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Occupancy_Profile {
    private static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm:ss a");
    private static DateTimeFormatter timeFormatterForReport = DateTimeFormatter.ofPattern("h:mm a");
    private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a");
    private static int vavNameColInReport = 1;
    private static int vavOccupancyStateInReport = 2;
    private static int occupancyProfileReport = 15;
    private static int occupancyProfileTrend = 30;
    private static int occupancyProfileSIS = 0;

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
            time = time.plusMinutes(15);
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

            workbook.save(fileName);

        } catch (Exception e){
            System.err.println("error in createOccupancyProfile " + e.getMessage());
        }
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

    /**
     * Get the worksheet with the provided name. Create a new one if worksheet doesnt exist
     * @param workbook
     * @param name
     * @return
     */
    private static Worksheet getWorksheetFromWorkbook(Workbook workbook, String name){
        Worksheet worksheet = workbook.getWorksheets().get(name);
        if (worksheet == null){
            worksheet = workbook.getWorksheets().add(name);
        }
        return worksheet;
    }

    public static void main(String[] args){
        String filename = System.getProperty("user.dir") + "/src/OCCUPANCY.xlsx";
        createOccupancyProfile(filename);
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

    private static void occupancyProfileForSISData(Workbook workbook) throws Exception {
        String roomName = "070-1650";
        String vavName = "338";
        Worksheet worksheet = getWorksheetFromWorkbook(workbook,"SIS data");
        Worksheet roomInputWorksheet = getWorksheetFromWorkbook(workbook, "Room Input data0");
        Worksheet occupancyProfileWorksheet = getWorksheetFromWorkbook(workbook,"OccupancyProfile");


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
    }

    private static void occupancyProfileForWebCtrlReportData(Workbook workbook) throws Exception {
        String vav = "70-VAV-338~";
        Worksheet inputWorksheet = getWorksheetFromWorkbook(workbook,"WebCtrlReportInput");
        Worksheet rawDataWorksheet = getWorksheetFromWorkbook(workbook,"ReportRawData");
        Worksheet occupancyProfileWorksheet = getWorksheetFromWorkbook(workbook,"OccupancyProfile");
        List<String[]> report = util.pullReportsFromWebCtrl(inputWorksheet, "Effective Schedule");
        util.saveReportToExcel(rawDataWorksheet, report);
        TreeMap<Integer, TreeMap<LocalTime, Integer>> keyDayOfWeek_valueOccupancyTable = createOccupancyTableMappingsFromReport(report, vav);

        multiplyOccupiedStateByMaxOccupancyValue(keyDayOfWeek_valueOccupancyTable, workbook);

        writeOccupancyTablesMappingstoExcel(occupancyProfileWorksheet, keyDayOfWeek_valueOccupancyTable, occupancyProfileReport, 0);
    }

    private static void multiplyOccupiedStateByMaxOccupancyValue(TreeMap<Integer, TreeMap<LocalTime, Integer>> keyDayOfWeek_valueOccupancyTable, Workbook workbook) throws Exception {
        String roomName = "070-1650";
        String vavName = "338";
        Worksheet sisDataWorksheet = getWorksheetFromWorkbook(workbook, "SIS data");
        Worksheet roomInputDataWorksheet = getWorksheetFromWorkbook(workbook, "Room Input data0");
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

    private static void occupancyProfileForWebCtrlTrendData(Workbook workbook) throws Exception {


        Worksheet inputWorksheet = getWorksheetFromWorkbook(workbook,"WebCtrl Input");
        Worksheet rawDataWorksheet = getWorksheetFromWorkbook(workbook,"Trend Data");
        Worksheet occupancyProfileWorksheet = getWorksheetFromWorkbook(workbook,"OccupancyProfile");

        String[] results = util.pullDataFromWebCtrl(inputWorksheet, "Occupancy Contact State");
        util.saveRawDataToExcel("Occupancy Contact State", rawDataWorksheet, 0, 0, results);
        TreeMap<Integer, TreeMap<LocalTime, Integer>> keyDayOfWeek_valueOccupancyTable = createEmptyOccupancyTableMappings();

        for (int i = 0; i < results.length; i+=2){
            LocalDateTime date = LocalDateTime.parse(results[i], dateTimeFormatter);
            Boolean isOn = results[i + 1].equalsIgnoreCase("0");

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
        multiplyOccupiedStateByMaxOccupancyValue(keyDayOfWeek_valueOccupancyTable, workbook);
        writeOccupancyTablesMappingstoExcel(occupancyProfileWorksheet, keyDayOfWeek_valueOccupancyTable, occupancyProfileTrend, 0);

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

}
