import Eval.EvalServiceLocator;
import Eval.EvalSoapBindingStub;
import ReportWSDL.ReportServiceLocator;
import ReportWSDL.ReportSoapBindingStub;
import Trend.TrendServiceLocator;
import Trend.TrendSoapBindingStub;
import com.aspose.cells.*;
import com.opencsv.CSVReader;
import io.github.cdimascio.dotenv.Dotenv;
import java.io.StringReader;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class util {
    private static final Dotenv dotenv = Dotenv.load();
    private static final String user = dotenv.get("USER");
    private static final String password = dotenv.get("PASS");
    private static final int nameCol = 0;
    private static final int pathCol = 1;
    private static final int startTimeCol = 2;
    private static final int endTimeCol = 3;
    private static final int limitFromStartCol = 4;
    private static final int maxRecordsCol = 5;
    private static final int reportNameCol = 2;
    private static final String ECONOMIZER = "070_ahu_03_econ_ok_tn";
    private static final String MAT = "070_ahu_03_ma_temp (℉)";
    private static final String SAT = "070_ahu_03_sa_temp (℉)";
    private static final String SACFM = "070_ahu_03_sa_air_flow (cfm)";
    private static final String OAT = "oa_temp  (℉)";
    private static final String PREHEAT = "070_ahu_03_phc_da_temp (℉)";
    private static final String CHW = "070_ahu_03_chw_valve (%open)";
    private static final String HW = "070_ahu_03_hw_valve (%open)";
    private static final int dateEnergyCol = 0;
    private static final int EconomizerEnergyCol = 1;
    private static final int MATEnergyCol = 2;
    private static final int SATEnergyCol = 3;
    private static final int OATEnergyCol = 5;
    private static final int PreheatEnergyCol = 6;
    private static final int QCoolingEnergyCol = 9;
    private static final int QHeatingEnergyCol = 10;
    private static final int CoolingEnergyCol = 11;
    private static final int HeatingEnergyCol = 12;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a");

    /**
     * Get the worksheet with the provided name. Create a new one if worksheet doesnt exist
     * @param workbook
     * @param name
     * @return
     */
    public static Worksheet getWorksheetFromWorkbook(Workbook workbook, String name){
        Worksheet worksheet = workbook.getWorksheets().get(name);
        if (worksheet == null){
            worksheet = workbook.getWorksheets().add(name);
        }
        return worksheet;
    }

    /**
     * TODO: Ignore for now
     * Make the graph for day
     * @param workbook the file to save the graph
     * @param names the names all the values for the day
     * @param sorted the sorted map where key is day and value is a list of values for that day
     */
    public static void makeDaySheet(Workbook workbook, ArrayList<String> names, LinkedHashMap<String, ArrayList<String>> sorted){
        try{
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a");
            // Example: key value pair for Monday
            //      2: [
            //          8/22/2022: [5, 6, 7],
            //          8/15/2022: [1, 2, 3.3]
            //      ]
            LinkedHashMap<Integer, LinkedHashMap<String, ArrayList<Double>>> dayMap = new LinkedHashMap<>();
            for (int i = 1; i <= 7; i++) {
                dayMap.put(i, new LinkedHashMap<>());
            }
            for (String period : sorted.keySet()) {
                ArrayList<Double> values = new ArrayList<>();
                for (String value : sorted.get(period)){
                    if (value.equalsIgnoreCase("")){
                        values.add(0.0);
                    } else {
                        values.add(Double.parseDouble(value));
                    }
                }
                LocalDateTime date = LocalDateTime.parse(period, formatter);
                dayMap.get(date.getDayOfWeek().getValue()).put(period, values);
            }

            // Get sum for each of the category for each day
            // Example, the sum of OATs on Monday
            LinkedHashMap<Integer, ArrayList<Double>> sums = new LinkedHashMap<>();

            for (int i : dayMap.keySet()) {
                ArrayList<Double> sum = new ArrayList<>();
                for (int a = 0; a < names.size(); a++){
                    sum.add(0.0);
                }
                sums.put(i, sum);
            }

            int count = 0;
            // in minutes
            ArrayList<Double> intervals = new ArrayList<>();
            // This first value will be ignored so that the list is 1-based index
            intervals.add(0.0);
            // Obtaining the interval
            for (int i : dayMap.keySet()){
                intervals.add(i, 0.0);
                if (count > 1){
                    count = 0;
                }
                LinkedHashMap<String, ArrayList<Double>> values = dayMap.get(i);
                for (String period : values.keySet()) {
                    LocalDateTime date = LocalDateTime.parse(period, formatter);
                    if (count == 0) {
                        intervals.set(i, (double) date.getMinute());
                    } else if (count == 1){
                        intervals.set(i, (date.getMinute() - intervals.get(i)) / 60 );
                    } else {
                        break;
                    }
                    count++;
                }
            }

            for (int i : dayMap.keySet()){
                LinkedHashMap<String, ArrayList<Double>> values = dayMap.get(i);
                Double interval = intervals.get(i);
                for (String period : values.keySet()){
                    for (int a = 0; a < values.get(period).size(); a++){
                        Double current = sums.get(i).get(a);
                        Double addition = values.get(period).get(a)*interval;
                        sums.get(i).set( a, current + ( addition ) );
                    }
                }
            }

            Worksheet worksheet = getWorksheetFromWorkbook(workbook, "Day Graph");
            Cells cells = worksheet.getCells();

            int col = 0;
            int row = 0;
            for (int i: dayMap.keySet()){
                cells.get(row, col).setValue(DayOfWeek.of(i).toString());
                col += names.size() + 1;
            }

            col = 0;
            row++;
            // leave 1 row for the sum


            for (int i : dayMap.keySet()){
                col = col + 1;
                for (Double sum : sums.get(i)){
                    cells.get(row, col).setValue(sum);
                    col = col + 1;
                }
            }

            col = 0;
            row++;
            for (int i : dayMap.keySet()){
                cells.get(row, col).setValue("Date");
                col += 1;
                for (String name : names) {
                    cells.get(row, col).setValue(name);
                    col += 1;
                }
            }
            col = 0;
            row++;
            int initialRow = row;
            int initialCol = col;
            for (int i : dayMap.keySet()) {
                row = initialRow;
                initialCol = col;
                LinkedHashMap<String, ArrayList<Double>> values = dayMap.get(i);
                for (String period : values.keySet()) {
                    cells.get(row, col).setValue(period);
                    col+=1;
                    for (Double value : values.get(period)){
                        cells.get(row, col).setValue(value);
                        col+=1;
                    }
                    row += 1;
                    col = initialCol;
                }
                col += names.size() + 1;
            }

            row = initialRow;
            for (int i : sums.keySet()) {
                cells.get(row, col+i).setValue(DayOfWeek.of(i).toString());
            }
            for (int idx = 0; idx < names.size(); idx++){
                row = row + 1;
                cells.get(row, col).setValue(names.get(idx));
                for (int i : sums.keySet()){
                    cells.get(row, col+i).setValue(sums.get(i).get(idx));
                }
            }
            row = initialRow;

        } catch (Exception e){
            System.err.println("Error in makeDaySheet " + e.getMessage());
        }
    }

    public static void makeDayGraph(String fileName){
        try{
            Workbook workbook = new Workbook(fileName);
            Worksheet worksheet = workbook.getWorksheets().get("Day Graph");
            int chartIndex = worksheet.getCharts().add(ChartType.BAR, 0, 0, 20, 30);
            Chart chart = worksheet.getCharts().get(chartIndex);
            chart.getTitle().setText("Day Of Week vs. Value");
            chart.getCategoryAxis().getTitle().setText("Day of Week");
            chart.getValueAxis().getTitle().setText("Value");
            chart.getNSeries();
            workbook.save(fileName);
        } catch (Exception e){
            System.err.println("makeDayGraph error " + e.getMessage());
        }
    }

    public static void makeMonthSheet(String location, ArrayList<String> names, LinkedHashMap<String, ArrayList<String>> sorted){
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a");
            LinkedHashMap<Integer, LinkedHashMap<LocalDateTime, ArrayList<Double>>> monthMap = new LinkedHashMap<>();
            for (int i = 1; i <= 12; i++) {
                monthMap.put(i, new LinkedHashMap<>());
            }
            for (String period : sorted.keySet()) {
                if (!sorted.get(period).contains("")) {
                    ArrayList<Double> values = new ArrayList<>();
                    for (String value : sorted.get(period)){
                        values.add(Double.parseDouble(value));
                    }
                    LocalDateTime date = LocalDateTime.parse(period, formatter);
                    monthMap.get(date.getMonthValue()).put(date, values);
                }
            }


            Workbook workbook = new Workbook(location);
            Worksheet worksheet = workbook.getWorksheets().add("Month Graph");
            Cells cells = worksheet.getCells();

            // Column index
            int col = 0;
            // Row index
            int row = 0;
            for (int i : monthMap.keySet()){
                cells.get(row, col).setValue(Month.of(i).toString());
                col += names.size() + 1;
            }
            col = 0;
            row++;
            // leave 1 row for the sum
            LinkedHashMap<Integer, ArrayList<Double>> sums = new LinkedHashMap<>();

            for (int i : monthMap.keySet()) {
                ArrayList<Double> sum = new ArrayList<>();
                for (int a = 0; a < names.size(); a++){
                    sum.add(0.0);
                }
                sums.put(i, sum);
            }
            int count = 0;
            // in minutes
            ArrayList<Double> intervals = new ArrayList<>();
            // This first value will be ignored so that the list is 1-based index
            intervals.add(0.0);
            // Obtaining the interval
            for (int i : monthMap.keySet()){
                intervals.add(i, 0.0);
                if (count > 1){
                    count = 0;
                }
                LinkedHashMap<LocalDateTime, ArrayList<Double>> values = monthMap.get(i);
                for (LocalDateTime date : values.keySet()) {
                    if (count == 0) {
                        intervals.set(i, (double) date.getMinute());
                    } else if (count == 1){
                        intervals.set(i, (date.getMinute() - intervals.get(i)) / 60 );
                    } else {
                        break;
                    }
                    count++;
                }
            }

            for (int i : monthMap.keySet()){
                LinkedHashMap<LocalDateTime, ArrayList<Double>> values = monthMap.get(i);
                Double interval = intervals.get(i);
                for (LocalDateTime date : values.keySet()){
                    for (int a = 0; a < values.get(date).size(); a++){
                        sums.get(i).set( a, sums.get(i).get(a) + ( values.get(date).get(a)*interval ) );
                    }
                }
            }
            for (int i : monthMap.keySet()){
                col = col + 1;
                for (Double sum : sums.get(i)){
                    cells.get(row, col).setValue(sum);
                    col = col + 1;
                }
            }

            col = 0;
            row++;
            for (int i : monthMap.keySet()){
                cells.get(row, col).setValue("Date");
                col += 1;
                for (String name : names) {
                    cells.get(row, col).setValue(name);
                    col += 1;
                }
            }
            col = 0;
            row++;
            int initialRow = row;
            int initialCol = col;
            for (int i : monthMap.keySet()) {
                row = initialRow;
                initialCol = col;
                LinkedHashMap<LocalDateTime, ArrayList<Double>> values = monthMap.get(i);
                for (LocalDateTime date : values.keySet()) {
                    cells.get(row, col).setValue(date);
                    col+=1;
                    for (Double value : values.get(date)){
                        cells.get(row, col).setValue(value);
                        col+=1;
                    }
                    row += 1;
                    col = initialCol;
                }
                col += names.size() + 1;
            }

            row = initialRow;
            for (int i : sums.keySet()) {
                cells.get(row, col+i).setValue(Month.of(i).toString());
            }
            for (int idx = 0; idx < names.size(); idx++){
                row = row + 1;
                cells.get(row, col).setValue(names.get(idx));
                for (int i : sums.keySet()){
                    cells.get(row, col+i).setValue(sums.get(i).get(idx));
                }
            }

            workbook.save(location);
        } catch (Exception e){
            System.err.println("makeMonthSheet error " + e.getMessage());
        }
    }

    public static String[] getDataFromTrend(String path, String stime, String etime, boolean limitFromStart, int maxRecords){
        try {
            TrendServiceLocator el = new TrendServiceLocator();

            TrendSoapBindingStub es = (TrendSoapBindingStub) el.getTrend();
            es.setPassword(password);
            es.setUsername(user);
            //String path = "#70-vav-301/air_flow/flow_input/trend_log";
            //String stime = "04/05/2022 9:00:00 AM";
            //String etime = "04/05/2022 10:00:00 PM";
            //Boolean limitFromStart = true;
            //int maxRecords = 200;
            return es.getTrendData(path, stime, etime, limitFromStart, maxRecords);
        } catch (Exception e) {
            System.err.println(e);
        }
        return null;
    }

    /**
     * Get the inputs for getting trend data from Excel
     * @param workbook the excel file where the inputs are
     * @return [names, paths, startTimes, endTimes, limitFromStarts, maxRecordss] is a list.
     * where each element is a list of things with said name.
     */
    public static ArrayList<ArrayList<String>> getTrendInputsFromExcel(Workbook workbook){
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> paths = new ArrayList<>();
        ArrayList<String> startTimes = new ArrayList<>();
        ArrayList<String> endTimes = new ArrayList<>();
        ArrayList<String> limitFromStarts = new ArrayList<>();
        ArrayList<String> maxRecordss = new ArrayList<>();
        try{
            Worksheet worksheet = getWorksheetFromWorkbook(workbook,"Trend");
            Cells cells = worksheet.getCells();
            int maxRow = cells.getLastDataRow(0);
            for (int i = 1; i <= maxRow; i++){
                names.add(cells.get(i, 0).getStringValue());
                paths.add(cells.get(i, 1).getStringValue());
                startTimes.add(cells.get(i,2).getStringValue());
                endTimes.add(cells.get(i,3).getStringValue());
                limitFromStarts.add(cells.get(i,4).getStringValue());
                maxRecordss.add(cells.get(i,5).getStringValue());
            }
            ArrayList<ArrayList<String>> results = new ArrayList<>();
            results.add(names);
            results.add(paths);
            results.add(startTimes);
            results.add(endTimes);
            results.add(limitFromStarts);
            results.add(maxRecordss);
            return results;
        } catch (Exception e){
            System.err.println("Error in getTrendInputsFromExcel " + e.getMessage());
        }
        return null;
    }

    public static void baseLineTask(Workbook workbook){
        try{
            ArrayList<ArrayList<String>> parameters = getTrendInputsFromExcel(workbook);
            LinkedHashMap<String, ArrayList<String>> sorted = new LinkedHashMap<>();
            ArrayList<String[]> data = new ArrayList<>();

            for (int i = 0; i < parameters.get(1).size(); i++){
                String[] pulledData = util.getDataFromTrend(parameters.get(1).get(i), parameters.get(2).get(i),
                        parameters.get(3).get(i), Boolean.parseBoolean(parameters.get(4).get(i)), Integer.parseInt(parameters.get(5).get(i)));
                data.add(pulledData);
                int a = 0;
                while (a < pulledData.length){
                    if (sorted.containsKey(pulledData[a])){
                        if (sorted.get(pulledData[a]).size() <= i) {
                            for (int d = sorted.get(pulledData[a]).size(); d < i; d++) {
                                sorted.get(pulledData[a]).add("");
                            }
                            sorted.get(pulledData[a]).add(pulledData[a + 1]);
                        }
                    } else {
                        ArrayList<String> valuesAtThisTime = new ArrayList<>();
                        for (int d = 0; d < i; d++){
                            valuesAtThisTime.add("");
                        }
                        valuesAtThisTime.add(pulledData[a+1]);
                        sorted.put(pulledData[a], valuesAtThisTime);
                    }

                    a+=2;
                }

            }

            ///// START OF TREND VALUES
            makeTrendValuesSheet(workbook, parameters.get(0), data);
            ////// END OF TREND VALUES

            ///// START OF TREND VALUES SORTED
            makeTrendValuesSortedSheet(workbook, parameters.get(0), sorted);
            ///// END OF TREND VALUES SORTED

//            LinkedHashMap<String, ArrayList<Double>> result1 = calculateEnergy(workbook, parameters.get(0), sorted);
            // TODO: Ignore for now
            // makeDaySheet(workbook, parameters.get(0), sorted);
//            makeMonthEnergySheet(workbook, result1);
//            makeDayEnergySheet(workbook, result1);

        } catch (Exception e){
            System.err.println("baseLineTask error "+e.getMessage());
        }
    }

    /**
     * This will pull data from WebCtrl and create 2 new Worksheet Trend Values and Trend Values Sorted
     * in which the data from WebCtrl are being stored.
     * Trend Values stores the raw data from WebCtrl.
     * Trend Values Sorted stores the data sorted by time from WebCtrl
     * @param workbook
     */
    public static void makeTrendValuesWorksheets(Workbook workbook){
        try{
            ArrayList<ArrayList<String>> parameters = getTrendInputsFromExcel(workbook);
            LinkedHashMap<String, ArrayList<String>> sorted = new LinkedHashMap<>();
            ArrayList<String[]> data = new ArrayList<>();

            for (int i = 0; i < parameters.get(1).size(); i++){
                String[] pulledData = util.getDataFromTrend(parameters.get(1).get(i), parameters.get(2).get(i),
                        parameters.get(3).get(i), Boolean.parseBoolean(parameters.get(4).get(i)), Integer.parseInt(parameters.get(5).get(i)));
                data.add(pulledData);
                int a = 0;
                while (a < pulledData.length){
                    if (sorted.containsKey(pulledData[a])){
                        if (sorted.get(pulledData[a]).size() <= i) {
                            for (int d = sorted.get(pulledData[a]).size(); d < i; d++) {
                                sorted.get(pulledData[a]).add("");
                            }
                            sorted.get(pulledData[a]).add(pulledData[a + 1]);
                        }
                    } else {
                        ArrayList<String> valuesAtThisTime = new ArrayList<>();
                        for (int d = 0; d < i; d++){
                            valuesAtThisTime.add("");
                        }
                        valuesAtThisTime.add(pulledData[a+1]);
                        sorted.put(pulledData[a], valuesAtThisTime);
                    }

                    a+=2;
                }
            }

            ///// START OF TREND VALUES
            makeTrendValuesSheet(workbook, parameters.get(0), data);
            ////// END OF TREND VALUES

            ///// START OF TREND VALUES SORTED
            makeTrendValuesSortedSheet(workbook, parameters.get(0), sorted);
            ///// END OF TREND VALUES SORTED

        } catch (Exception e){
            System.err.println("Error in pulling TrendValues data "+e.getMessage());
        }
    }

    private static void makeMonthEnergySheet(Workbook workbook,LinkedHashMap<String, ArrayList<Double>> energy ){
        int HDDCol = 1;
        int CDDCol = 2;
        int CoolCol = 4;
        int HeatCol = 3;
        int monthCol = 0;
        LinkedHashMap<Integer, Double> cooling = new LinkedHashMap<>();
        for (int i = 1; i < 13; i++){
            cooling.put(i, 0.0);
        }
        LinkedHashMap<Integer, Double> heating = new LinkedHashMap<>();
        for (int i = 1; i < 13; i++){
            heating.put(i, 0.0);
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a");
        for (String date: energy.keySet()){
            Double coolingEnergy = energy.get(date).get(0);
            Double heatingEnergy = 0.0;
            if (energy.get(date).size() > 1){
                heatingEnergy = energy.get(date).get(1);
            }
            Integer currentMonth = LocalDateTime.parse(date, formatter).getMonthValue();
            cooling.put(currentMonth, cooling.get(currentMonth) + coolingEnergy);
            heating.put(currentMonth, heating.get(currentMonth) + heatingEnergy);
        }
        try {
            Worksheet worksheet = workbook.getWorksheets().get("Month Degree Days");
            Cells cells = worksheet.getCells();
            cells.get(0, CoolCol).setValue("Month Cooling energy (thousand Btu)");
            for (Integer i : cooling.keySet()) {
                cells.get(i, CoolCol).setValue(cooling.get(i));
            }
            cells.get(0, HeatCol).setValue("Month Heating energy (thousand Btu)");
            for (Integer i : heating.keySet()) {
                cells.get(i,HeatCol).setValue(heating.get(i));
            }
        } catch (Exception e) {
            System.err.println("makeMonthEnergySheet error "+ e.getMessage());
        }
    }

    /**
     * Calculating Energy according to ASHRAE standards
     * @param QValues The key is date. The value is a list of q values: Cooling Q Value, Heating Q Value
     * @return The key of this mapping is the date. the value of this mapping is a list of values: Cooling Energy, Heating Energy
     * @throws Exception
     */
    public static LinkedHashMap<String, ArrayList<Double>> calculateEnergy(LinkedHashMap<String, ArrayList<Double>> QValues) {
        // Getting time interval
        int count = 0;
        Double interval = 0.0;
        for (String date: QValues.keySet()){
            if (count == 0){
                LocalDateTime currentDate = LocalDateTime.parse(date, formatter);
                interval = (double) currentDate.getMinute();
            } else if (count == 1){
                LocalDateTime currentDate = LocalDateTime.parse(date, formatter);
                interval = currentDate.getMinute() - interval;
            } else {
                break;
            }
            count++;
        }
        // Finish getting time interval

        LinkedHashMap<String, ArrayList<Double>> energy = new LinkedHashMap<>();

        for (String date: QValues.keySet()){
            energy.put(date, new ArrayList<>());
            energy.get(date).add(0, -1 * QValues.get(date).get(0) * interval / 1000);
            energy.get(date).add(1, QValues.get(date).get(1) * interval / 1000);
        }

        return energy;
    }

    /**
     * Calculating Q values according to ASHRAE standards
     * @param names The names associated with each values in the list of values above
     * @param sorted The key of this mapping is the date. The value of this mapping is a list of values.
     * @return The key is date. The value is a list of q values: Cooling Q Value, Heating Q Value
     * @throws Exception
     */
    public static LinkedHashMap<String, ArrayList<Double>> calculateQValues(ArrayList<String> names, LinkedHashMap<String, ArrayList<String>> sorted) throws Exception {
        int totalVariables = 8;
        int econ = 0;
        int hw = 0;
        int chw = 0;
        int mat = 0;
        int sat = 0;
        int sacfm = 0;
        int oat = 0;
        int preheat = 0;
        for (int i = 0; i < names.size(); i++){
            String name = names.get(i);
            if (name.equalsIgnoreCase(ECONOMIZER)){
                econ = i;
            } else if (name.equalsIgnoreCase(MAT)){
                mat = i;
            } else if (name.equalsIgnoreCase(SAT)){
                sat = i;
            } else if (name.equalsIgnoreCase(SACFM)){
                sacfm = i;
            } else if (name.equalsIgnoreCase(OAT)){
                oat = i;
            } else if (name.equalsIgnoreCase(PREHEAT)){
                preheat = i;
            } else if (name.equalsIgnoreCase(CHW)){
                chw = i;
            } else if (name.equalsIgnoreCase(HW)){
                hw = i;
            }
        }
        if (new HashSet<>(Arrays.asList(econ, mat, sat, sacfm, oat, preheat, hw, chw)).size() != totalVariables){
            throw new Exception("Not enough arguments!");
        }
        // Getting Q values
        LinkedHashMap<String, ArrayList<Double>> qValues = new LinkedHashMap<>();

        for (String date : sorted.keySet()){
            ArrayList<String> currentValues = sorted.get(date);
            if (currentValues.size() >= totalVariables&&
                    !currentValues.get(oat).equalsIgnoreCase("") &&
                    !currentValues.get(mat).equalsIgnoreCase("") &&
                    !currentValues.get(sat).equalsIgnoreCase("") &&
                    !currentValues.get(econ).equalsIgnoreCase("") &&
                    !currentValues.get(sacfm).equalsIgnoreCase("") &&
                    !currentValues.get(preheat).equalsIgnoreCase("")) {
                Double chwValue = Double.parseDouble(currentValues.get(chw));
                Double hwValue = Double.parseDouble(currentValues.get(hw));
                Double satValue = Double.parseDouble(currentValues.get(sat));
                Double sacfmValue = Double.parseDouble(currentValues.get(sacfm));
                Double preheatValue = Double.parseDouble(currentValues.get(preheat));
                Double matValue = Double.parseDouble(currentValues.get(mat));
                Double qCoolingValue = 0.0;
                Double qHeatingValue = 0.0;

                // Calculating q Cooling
                if (chwValue > 0){
                    qCoolingValue = Math.min(0,
                            0.01791 * sacfmValue * ( satValue - preheatValue )
                    );
                }
                // Calculating q Heating
                if (hwValue > 0){
                    qHeatingValue = Math.max(0,
                            0.01791 * sacfmValue * ( preheatValue - matValue )
                            );
                }

                qValues.put(date, new ArrayList<>(Arrays.asList(qCoolingValue, qHeatingValue)));
            }
        }
        return qValues;
    }


    public static void writeEnergy(Worksheet worksheet, ArrayList<String> names, LinkedHashMap<String, ArrayList<Double>> keyDate_values, int srow, int scol){
        Cells cells = worksheet.getCells();
        int row = srow;
        int col = scol;
        cells.get(srow, scol).setValue("Date");
        for (int i = 0; i < names.size(); i++){
            cells.get(row, col + i + 1).setValue(names.get(i));
        }
        for (String date: keyDate_values.keySet()){
            row++;
            cells.get(row, col).setValue(date);
            ArrayList<Double> values = keyDate_values.get(date);
            for (int i = 0; i< values.size(); i++){
                cells.get(row, col + i + 1).setValue(values.get(i));
            }
        }
    }

    public static void makeTrendValuesSheet(Workbook workbook, ArrayList<String> names, ArrayList<String[]> data){
        try {
            Worksheet worksheet = getWorksheetFromWorkbook(workbook,"Trend Values");
            Cells cells = worksheet.getCells();
            int z = 0;
            for (String name : names){
                cells.get(0, z).setValue("Date");
                cells.get(0, z+1).setValue(name);
                z+=2;
            }

            int colIdx = 0;
            int rowIdx = 1;
            for (String[] datum: data){
                int idx = 0;
                while (idx < datum.length){
                    cells.get(rowIdx, colIdx).setValue(datum[idx]);
                    cells.get(rowIdx, colIdx+1).setValue(datum[idx+1]);
                    idx+=2;
                    rowIdx++;
                }
                colIdx+=2;
                rowIdx = 1;
            }
        } catch (Exception e){
            System.err.println("makeTrendValuesSheet " + e.getMessage());
        }
    }

    public static void makeTrendValuesSortedSheet(Workbook workbook, ArrayList<String> names, LinkedHashMap<String, ArrayList<String>> sorted ){
        try {
            Worksheet worksheet = getWorksheetFromWorkbook(workbook,"Trend Values Sorted");
            Cells cells = worksheet.getCells();

            int z = 0;
            cells.get(0, z).setValue("Date");
            z = 1;
            for (String name : names){
                cells.get(0, z).setValue(name);
                z++;
            }

            int i = 1;
            for (String period : sorted.keySet()){
                cells.get(i,0).setValue(period);
                int idx = 1;
                for (String value : sorted.get(period)){
                    cells.get(i,idx).setValue(value);
                    idx++;
                }
                i++;
            }

        } catch (Exception e){
            System.err.println("makeTrendValuesSheet " + e.getMessage());
        }
    }

    public static void setEval(String path, Double newValue, String reason) {
        try {
            EvalServiceLocator el = new EvalServiceLocator();
            EvalSoapBindingStub es = (EvalSoapBindingStub) el.getEval();

            es.setPassword(password);
            es.setUsername(user);
            // es.setValue(path, newValue.toString(), reason);
            System.out.println(path);
            System.out.println(newValue.toString());
            System.out.println(reason);

            System.out.println(es.getValue(path));
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    private static String getReportsFromWebCtrl(String location, String reportName){
        try {
            ReportServiceLocator el = new ReportServiceLocator();
            ReportSoapBindingStub es = (ReportSoapBindingStub) el.getReport();

            es.setPassword(password);
            es.setUsername(user);
            return es.runReport(location, reportName, "CSV");

        } catch (Exception e) {
            System.err.println("error in getReportsFromWebCtrl " + e.getMessage());
        }
        return null;
    }

    public static void makeDayEnergySheet(Workbook workbook, LinkedHashMap<String, ArrayList<Double>> energy){
        TreeMap<Integer, Double> keyDayOfYear_valueCoolingEnergy = new TreeMap<>();
        TreeMap<Integer, Double> keyDayOfYear_valueHeatingEnergy = new TreeMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a");
        for (String period: energy.keySet() ){
            LocalDateTime date = LocalDateTime.parse(period, formatter);
            if (keyDayOfYear_valueCoolingEnergy.containsKey(date.getDayOfYear())){
                Double current = keyDayOfYear_valueCoolingEnergy.get(date.getDayOfYear());
                keyDayOfYear_valueCoolingEnergy.put(date.getDayOfYear(), current + energy.get(period).get(0));
            } else {
                keyDayOfYear_valueCoolingEnergy.put(date.getDayOfYear(), energy.get(period).get(0));
            }
            if (keyDayOfYear_valueHeatingEnergy.containsKey(date.getDayOfYear())){
                Double current = keyDayOfYear_valueHeatingEnergy.get(date.getDayOfYear());
                keyDayOfYear_valueHeatingEnergy.put(date.getDayOfYear(), current + energy.get(period).get(1));
            } else {
                keyDayOfYear_valueHeatingEnergy.put(date.getDayOfYear(), energy.get(period).get(1));
            }
        }
        try {
            int dateCol = 0;
            int CDDCol = 2;
            int HDDCol = 3;
            int CoolingCol= 5;
            int HeatingCol = 4;

            Worksheet worksheet = getWorksheetFromWorkbook(workbook,"Degree Days");
            Cells cells = worksheet.getCells();
            cells.get(0, CoolingCol).setValue("Cooling Energy (thousand Btu)");
            cells.get(0,HeatingCol).setValue("Heating Energy (thousand Btu)");
            for (int i = 1; i <= cells.getLastDataRow(dateCol); i++){
                String date = cells.get(i, dateCol).getStringValue();
                LocalDate localDate = LocalDate.parse(date);
                int dayOfYear = localDate.getDayOfYear();
                cells.get(i, CoolingCol).setValue(keyDayOfYear_valueCoolingEnergy.get(dayOfYear));
                cells.get(i, HeatingCol).setValue(keyDayOfYear_valueHeatingEnergy.get(dayOfYear));
            }
        } catch (Exception e) {
            System.err.println("Error in makeDayEnergySheet " + e.getMessage());
        }
    }

    /**
     * This will get the necessary inputs from the specified row (with the specified rowName) and pull data from WebCtrl
     * @param worksheet the worksheet we are working with
     * @param rowName the name of the row where the inputs are
     * @return the data from WebCtrl
     */
    public static String[] pullDataFromWebCtrl(Worksheet worksheet, String rowName) throws Exception {
        Cells cells = worksheet.getCells();
        int rowIndex = -1;
        for (int i = 0; i <= cells.getMaxDataRow(); i++){
            if (cells.get(i, nameCol).getStringValue().equalsIgnoreCase(rowName)){
                rowIndex = i;
                break;
            }
        }
        if (rowIndex == -1){
            throw new Exception("No such row names to pullDataFromWebCtrl");
        }
        String path = cells.get(rowIndex, pathCol).getStringValue();
        String startTime = cells.get(rowIndex, startTimeCol).getStringValue();
        String endTime = cells.get(rowIndex, endTimeCol).getStringValue();
        Boolean limitFromStart = cells.get(rowIndex, limitFromStartCol).getBoolValue();
        Integer maxRecords = cells.get(rowIndex, maxRecordsCol).getIntValue();
        return getDataFromTrend(path, startTime, endTime, limitFromStart, maxRecords);
    }


    /**
     * This will be used to save data returned from WebCtrl
     * @param worksheet the worksheet where the data will be saved to
     * @param col the column where the data should start
     * @param row the row where the data should start
     * @param data the data in the format: [time1, value1, time2, value2, ...., timeN, valueN)
     * @param name the name of the data
     */
    public static void saveRawDataToExcel(String name, Worksheet worksheet, int col, int row, String[] data){
        Cells cells = worksheet.getCells();
        int currentRow = row;
        cells.get(currentRow, col).setValue("Date");
        cells.get(currentRow, col+1).setValue(name);
        currentRow++;
        for (int i = 0; i < data.length; i+=2 ){
            cells.get(currentRow, col).setValue(data[i]);
            cells.get(currentRow, col+1).setValue(data[i+1]);
            currentRow++;
        }
    }

    public static List<String[]> pullReportsFromWebCtrl(Worksheet worksheet, String rowName) throws Exception {
        Cells cells = worksheet.getCells();
        int rowIndex = -1;
        for (int i = 0; i <= cells.getMaxDataRow(); i++){
            if (cells.get(i, nameCol).getStringValue().equalsIgnoreCase(rowName)){
                rowIndex = i;
                break;
            }
        }
        if (rowIndex == -1){
            throw new Exception("No such row names to pullDataFromWebCtrl");
        }
        String location = cells.get(rowIndex, pathCol).getStringValue();
        String reportName = cells.get(rowIndex, reportNameCol).getStringValue();
        String report = getReportsFromWebCtrl(location, reportName);
        assert report != null;
        CSVReader reader = new CSVReader(new StringReader(report));
        return reader.readAll();
    }

    public static void saveReportToExcel(Worksheet worksheet, List<String[]> report){
        Cells cells = worksheet.getCells();
        int initialCol = 0;
        int row = 0;
        int col = 0;
        for (String[] line: report){
            for (String item: line){
                cells.get(row, col).setValue(item);
                col++;
            }
            col = initialCol;
            row++;
        }
    }

    public static void syncOccupancyTableDataWithEnergyData(Worksheet energyWorksheet, String columnName, TreeMap<Integer, TreeMap<LocalTime, Integer>> ccupancyTablesMapping){
        Cells cells = energyWorksheet.getCells();
        int col = cells.getMaxDataColumn()+1;
        cells.get(0, col).setValue(columnName);
        cells.get(0, col+1).setValue(columnName+ " occupancy status");
        for (int i = 1; i <= cells.getMaxDataRow(); i++){
            LocalDateTime currentTime = LocalDateTime.parse(cells.get(i, dateEnergyCol).getStringValue(), formatter);
            TreeMap<LocalTime, Integer> currentOccupancyTable = ccupancyTablesMapping.get(currentTime.getDayOfWeek().getValue());
            if (currentOccupancyTable.containsKey(currentTime.toLocalTime())){
                if (currentOccupancyTable.get(currentTime.toLocalTime()) > 0){
                    cells.get(i, col+1).setValue("Occupied");
                } else {
                    cells.get(i, col+1).setValue("Unoccupied");
                }
                cells.get(i, col).setValue(currentOccupancyTable.get(currentTime.toLocalTime()));
            }
        }
    }

    /**
     * Return the following mapping with the following example structure:
     * 1 (the Occupancy Value/Status):
     *      01/01/2018 09:00:00 AM: [16.2, 30.5, 303.5],
     *      01/01/2018 09:05:00 AM: [16.2, 30.5, 303.5],
     * @param col the column where the occupancy value is
     * @param worksheet the worksheet where all the data is
     */
    public static TreeMap<Integer, TreeMap<LocalDateTime, ArrayList<Double>>> createOATvsEnergyBasedOnOccupancyValue(Worksheet worksheet, int col){
        TreeMap<Integer, TreeMap<LocalDateTime, ArrayList<Double>>> result = new TreeMap<>();
        Cells cells = worksheet.getCells();
        for (int i = 1; i < cells.getMaxDataRow(); i++){
            Integer currentOccupancyValue = cells.get(i, col).getIntValue();
            Integer currentGroup = 0;
            if (currentOccupancyValue> 0){
                currentGroup = 1;
            } else if (currentGroup == 0){
                currentGroup = 0;
            } else {
                currentGroup = -1;
            }
            LocalDateTime currentDateTime = LocalDateTime.parse(cells.get(i, dateEnergyCol).getStringValue(), formatter);
            Double currentOAT = Double.parseDouble(cells.get(i, OATEnergyCol).getStringValue());
            Double currentCoolingEnergy = cells.get(i, CoolingEnergyCol).getDoubleValue();
            Double currentHeatingEnergy = cells.get(i, HeatingEnergyCol).getDoubleValue();
            if (!result.containsKey(currentGroup)){
                result.put(currentGroup, new TreeMap<>());
            }
            result.get(currentGroup).put(currentDateTime, new ArrayList<>(Arrays.asList(currentOAT, currentCoolingEnergy, currentHeatingEnergy)));
        }
        return result;
    }

    /**
     * Get the column index of whichever column you need
     * @param colName the column name that you are looking for
     * @param worksheet the worksheet
     * @param row the row that the column is on
     * @return
     */
    public static int getColumnIndexOf( String colName, Worksheet worksheet, int row){
        Cells cells = worksheet.getCells();
        int col = -1;
        for (int i = 0; i <= cells.getMaxDataColumn(); i++){
            if (cells.get(row, i).getStringValue().equalsIgnoreCase(colName)){
                col = i;
                break;
            }
        }

        return col;

    }


    public static void writeOATvsEnergyToExcel(Worksheet worksheet, String name, TreeMap<Integer, TreeMap<LocalDateTime, ArrayList<Double>>> mapping, int row, int col){
        Cells cells = worksheet.getCells();
        int initialRow = row;
        for (Integer occupancyValue: mapping.keySet()){
            TreeMap<LocalDateTime, ArrayList<Double>> keyTime_valueOATCoolHeat = mapping.get(occupancyValue);
            cells.get(row, col).setValue(name + " occupancy value " + occupancyValue);
            cells.get(row, col+1).setValue("OAT (°F)");
            cells.get(row, col+2).setValue("Cooling Energy (thousand Btu)");
            cells.get(row, col+3).setValue("Heating Energy (thousand Btu)");

            for (LocalDateTime time: keyTime_valueOATCoolHeat.keySet()){
                row++;
                ArrayList<Double> currentValues = keyTime_valueOATCoolHeat.get(time);
                cells.get(row, col).setValue(time.toString());
                cells.get(row, col+1).setValue(currentValues.get(0));
                cells.get(row, col+2).setValue(currentValues.get(1));
                cells.get(row, col+3).setValue(currentValues.get(2));
            }
            col+=4;
            row = initialRow;
        }
    }

    /**
     *  Get the names of all the trend values in this worksheet
     *  Starting from column B, row 1 of the worksheet
     * @param worksheet
     * @return
     */
    public static ArrayList<String> getTrendNames(Worksheet worksheet){
        Cells cells = worksheet.getCells();
        ArrayList<String> result = new ArrayList<>();
        for (int i = 1; i <= cells.getMaxDataColumn(); i++){
            result.add(cells.get(0, i).getStringValue());
        }
        return result;
    }

    /**
     * Read the data from the provided worksheet and translate it to a structure that code understands
     * @param worksheet
     * @return
     */
    public static LinkedHashMap<String, ArrayList<String>> getSortedTrendValues(Worksheet worksheet){
        LinkedHashMap<String, ArrayList<String>> sorted = new LinkedHashMap<>();
        Cells cells = worksheet.getCells();
        int maxRow = cells.getMaxDataRow();
        int maxCol = cells.getMaxDataColumn();
        for (int row = 1; row <= maxRow; row++){
            ArrayList<String> values = new ArrayList<>();
            String date = cells.get(row, dateEnergyCol).getStringValue();
            for (int col = 1; col <= maxCol; col++){
                values.add(cells.get(row, col).getStringValue());
            }
            sorted.put(date, values);
        }
        return sorted;
    }

    public static LinkedHashMap<String, ArrayList<Double>> combineStringHashMapAndDoubleHashMap(LinkedHashMap<String, ArrayList<String>> sorted, LinkedHashMap<String, ArrayList<Double>> energy){
        LinkedHashMap<String, ArrayList<Double>> result = new LinkedHashMap<>();
        for (String date: energy.keySet()){
            ArrayList<Double> values = new ArrayList<>();
            for (String value: sorted.get(date)){
                if (!value.equalsIgnoreCase("")){
                    values.add(Double.parseDouble(value));
                } else {
                    values.add(0.0);
                }
            }
            values.addAll(energy.get(date));
            result.put(date, values);
        }
        return result;
    }

    public static LinkedHashMap<String, ArrayList<Double>> combineHashMaps(LinkedHashMap<String, ArrayList<Double>> list1, LinkedHashMap<String, ArrayList<Double>> list2){
        LinkedHashMap<String, ArrayList<Double>> result = new LinkedHashMap<>();
        for (String date: list1.keySet()){
            ArrayList<Double> values = new ArrayList<>();
            values.addAll(list1.get(date));
            values.addAll(list2.get(date));
            result.put(date, values);
        }
        return result;
    }

    public static LinkedHashMap<String, ArrayList<Double>> combineDoubleHashMapWithTreeMap(LinkedHashMap<String, ArrayList<Double>> list1, TreeMap<String, Double> list2){
        LinkedHashMap<String, ArrayList<Double>> result = new LinkedHashMap<>();
        for (String date: list1.keySet()){
            ArrayList<Double> values = new ArrayList<>();
            values.addAll(list1.get(date));
            values.add(list2.get(date));
            result.put(date, values);
        }
        return result;
    }

    /**
     *
     * @param worksheet
     * @param names
     * @param allData the data with Q values and energy
     */
    public static void calculateAndWriteHourlyData(Worksheet worksheet, ArrayList<String> names, LinkedHashMap<String, ArrayList<Double>> allData){
        LinkedHashMap<LocalDateTime, LinkedHashMap<String, ArrayList<Double>>> hourlyData = calculateHourlyData(allData);
        writeHourlyData(worksheet, names, hourlyData);
    }

    public static LinkedHashMap<LocalDateTime, LinkedHashMap<String, ArrayList<Double>>> calculateHourlyData(LinkedHashMap<String, ArrayList<Double>> allData){
        LinkedHashMap<LocalDateTime, LinkedHashMap<String, ArrayList<Double>>> hourlyData = new LinkedHashMap<>();
        for (String dateString: allData.keySet()){
            LocalDateTime date = LocalDateTime.parse(dateString, formatter);
            LocalDateTime newDate = LocalDateTime.of(date.getYear(), date.getMonthValue(), date.getDayOfMonth(), date.getHour(), 0);
            if (!hourlyData.containsKey(newDate)){
                hourlyData.put(newDate, new LinkedHashMap<>());
            }
            hourlyData.get(newDate).put(dateString, allData.get(dateString));
        }
        return hourlyData;
    }

    public static void writeHourlyData(Worksheet worksheet, ArrayList<String> names, LinkedHashMap<LocalDateTime, LinkedHashMap<String, ArrayList<Double>>> hourlyData){
        Cells cells = worksheet.getCells();
        cells.get(0, 0).setValue("Date");
        for (int i = 0; i< names.size(); i++){
            cells.get(0, i+1).setValue(names.get(i));
        }
        int row = 1;

        for (LocalDateTime time : hourlyData.keySet()){
            cells.get(row, 0).setValue(time.toString());
            LinkedHashMap<String, ArrayList<Double>> keyDate_values = hourlyData.get(time);
            for (int i = 0; i <names.size(); i++){
                boolean isTemperature = names.get(i).contains("℉");
                Double result;
                if (isTemperature){
                    int n = keyDate_values.keySet().size();
                    Double total = 0.0;
                    for (String date: keyDate_values.keySet()){
                        total += keyDate_values.get(date).get(i);
                    }
                    result = total / n;
                } else {
                    Double total = 0.0;
                    for (String date: keyDate_values.keySet()){
                        total += keyDate_values.get(date).get(i);
                    }
                    result = total;
                }
                cells.get(row, i+1).setValue(result);
            }
            row++;
        }
    }

    public static void writeDailyData(Worksheet worksheet, ArrayList<String> names, LinkedHashMap<LocalDate, LinkedHashMap<String, ArrayList<Double>>> dailyData, LinkedHashMap<String, ArrayList<Double>> NOAAData, int srow, int scol){
        Cells cells = worksheet.getCells();
        cells.get(srow, scol).setValue("Date");
        for (int i = 0; i< names.size(); i++){
            cells.get(srow, scol+i+1).setValue(names.get(i));
        }
        int row = srow+1;

        for (LocalDate time : dailyData.keySet()){
            cells.get(row, scol).setValue(time.toString());
            LinkedHashMap<String, ArrayList<Double>> keyDate_values = dailyData.get(time);
            for (int i = 0; i < names.size(); i++){
                String name = names.get(i);
                if (!NOAAData.containsKey(time.toString())){
                    NOAAData.put(time.toString(), new ArrayList<>(Arrays.asList(0.0,0.0,0.0)));
                }
                if (name.equalsIgnoreCase("NOAA Average Temperature (℉)")){
                    cells.get(row, scol+i+1).setValue(NOAAData.get(time.toString()).get(0));
                } else if (name.equalsIgnoreCase("NOAA Cooling Degree Days (℉.day)")){
                    cells.get(row, scol+i+1).setValue(NOAAData.get(time.toString()).get(1));
                } else if (name.equalsIgnoreCase("NOAA Heating Degree Days (℉.day)")){
                    cells.get(row, scol+i+1).setValue(NOAAData.get(time.toString()).get(2));
                } else {
                    boolean isTemperature = name.contains("℉");
                    Double result;
                    if (isTemperature){
                        int n = keyDate_values.keySet().size();
                        Double total = 0.0;
                        for (String date: keyDate_values.keySet()){
                            total += keyDate_values.get(date).get(i);
                        }
                        result = total / n;
                    } else {
                        Double total = 0.0;
                        for (String date: keyDate_values.keySet()){
                            total += keyDate_values.get(date).get(i);
                        }
                        result = total;
                    }
                    cells.get(row, scol+i+1).setValue(result);
                }
            }
            row++;
        }
    }
    public static void calculateAndWriteDailyData(Worksheet worksheet, Worksheet NOAAInput, ArrayList<String> names, LinkedHashMap<String, ArrayList<Double>> allData, int srow, int scol){
        LinkedHashMap<LocalDate, LinkedHashMap<String, ArrayList<Double>>> dailyData = calculateDailyData(allData);
        LinkedHashMap<String, ArrayList<Double>> NOAAData = APICALL.getDataFromExcel(NOAAInput);
        ArrayList<String> newNames = new ArrayList<>();
        newNames.addAll(names);
        newNames.add("NOAA Average Temperature (℉)");
        newNames.add("NOAA Cooling Degree Days (℉.day)");
        newNames.add("NOAA Heating Degree Days (℉.day)");
        writeDailyData(worksheet, newNames, dailyData, NOAAData, srow, scol);
    }

    public static LinkedHashMap<Month, LinkedHashMap<String, ArrayList<Double>>> calculateMonthlyData(LinkedHashMap<String, ArrayList<Double>> allData){
        LinkedHashMap<Month, LinkedHashMap<String, ArrayList<Double>>> monthlyData = new LinkedHashMap<>();
        for (String dateString: allData.keySet()){
            LocalDateTime date = LocalDateTime.parse(dateString, formatter);
            Month month = Month.of(date.getMonthValue());
            if (!monthlyData.containsKey(month)){
                monthlyData.put(month, new LinkedHashMap<>());
            }
            monthlyData.get(month).put(dateString, allData.get(dateString));
        }
        return monthlyData;
    }

    public static void writeMonthlyData(Worksheet worksheet, ArrayList<String> names, LinkedHashMap<Month, ArrayList<Double>> monthlyNOAAData, LinkedHashMap<Month, LinkedHashMap<String, ArrayList<Double>>> monthlyData, int srow, int scol){
        Cells cells = worksheet.getCells();
        cells.get(srow, scol).setValue("Date");
        for (int i = 0; i < names.size(); i++){
            cells.get(srow, scol + i + 1).setValue(names.get(i));
        }
        int row = srow+1;
        for (Month month : monthlyData.keySet()){
            cells.get(row, scol).setValue(month.toString());
            LinkedHashMap<String, ArrayList<Double>> keyDate_values = monthlyData.get(month);
            for (int i = 0; i < names.size(); i++){
                String name = names.get(i);
                if (!monthlyNOAAData.containsKey(month)){
                    monthlyNOAAData.put(month, new ArrayList<>(Arrays.asList(0.0,0.0,0.0)));
                }
                if (name.equalsIgnoreCase("NOAA Average Temperature (℉)")){
                    cells.get(row, scol+i+1).setValue(monthlyNOAAData.get(month).get(0));
                } else if (name.equalsIgnoreCase("NOAA Cooling Degree Days (℉.day/month)")){
                    cells.get(row, scol+i+1).setValue(monthlyNOAAData.get(month).get(1));
                } else if (name.equalsIgnoreCase(("NOAA Heating Degree Days (℉.day/month)"))){
                    cells.get(row, scol+i+1).setValue(monthlyNOAAData.get(month).get(2));
                } else {
                    boolean isTemperature = name.contains("℉");
                    Double result;
                    if (isTemperature){
                        int n = keyDate_values.keySet().size();
                        Double total = 0.0;
                        for (String date: keyDate_values.keySet()){
                            total += keyDate_values.get(date).get(i);
                        }
                        result = total / n;
                    } else {
                        Double total = 0.0;
                        for (String date: keyDate_values.keySet()){
                            total += keyDate_values.get(date).get(i);
                        }
                        result = total;
                    }
                    cells.get(row, scol+i+1).setValue(result);
                }
            }
            row++;
        }
    }

    private static LinkedHashMap<Month, ArrayList<Double>> convertNOAADataToMonthly(LinkedHashMap<String, ArrayList<Double>> NOAAData){
        LinkedHashMap<Month, LinkedHashMap<String, ArrayList<Double>>> monthlyData = new LinkedHashMap<>();
        for (String date: NOAAData.keySet()){
            LocalDate currentDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
            if (!monthlyData.containsKey(currentDate.getMonth())){
                monthlyData.put(currentDate.getMonth(), new LinkedHashMap<>());
            }
            monthlyData.get(currentDate.getMonth()).put(date, NOAAData.get(date));
        }
        LinkedHashMap<Month, ArrayList<Double>> result = new LinkedHashMap<>();
        for (Month month: monthlyData.keySet()){
            LinkedHashMap<String, ArrayList<Double>> keyDate_values = monthlyData.get(month);
            ArrayList<Double> endResult = new ArrayList<>();

            // AVerage temperature
            Double total = 0.0;
            for (String date : keyDate_values.keySet()){
                total += keyDate_values.get(date).get(0);
            }
            int n = keyDate_values.keySet().size();
            endResult.add(total/ n);


            // CDD
            total = 0.0;
            for (String date : keyDate_values.keySet()){
                total += keyDate_values.get(date).get(1);
            }
            endResult.add(total);
            // HDD
            total = 0.0;
            for (String date : keyDate_values.keySet()){
                total += keyDate_values.get(date).get(2);
            }
            endResult.add(total);
            result.put(month, endResult);
        }
        return result;
    }

    public static void calculateAndWriteMonthlyData(Worksheet worksheet, Worksheet NOAAInput, ArrayList<String> names, LinkedHashMap<String, ArrayList<Double>> allData, int srow, int scol){
        LinkedHashMap<Month, LinkedHashMap<String, ArrayList<Double>>> monthlyData = calculateMonthlyData(allData);
        LinkedHashMap<String, ArrayList<Double>> NOAAData = APICALL.getDataFromExcel(NOAAInput);
        LinkedHashMap<Month, ArrayList<Double>> monthlyNOAAData = convertNOAADataToMonthly(NOAAData);
        ArrayList<String> newNames = new ArrayList<>();
        newNames.addAll(names);
        newNames.add("NOAA Average Temperature (℉)");
        newNames.add("NOAA Cooling Degree Days (℉.day/month)");
        newNames.add("NOAA Heating Degree Days (℉.day/month)");
        writeMonthlyData(worksheet, newNames, monthlyNOAAData, monthlyData, srow, scol);
    }

    public static LinkedHashMap<LocalDate, LinkedHashMap<String, ArrayList<Double>>> calculateDailyData(LinkedHashMap<String, ArrayList<Double>> allData){
        LinkedHashMap<LocalDate, LinkedHashMap<String, ArrayList<Double>>> dailyData = new LinkedHashMap<>();
        for (String dateString: allData.keySet()){
            LocalDateTime date = LocalDateTime.parse(dateString, formatter);
            LocalDate newDate = LocalDate.of(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
            if (!dailyData.containsKey(newDate)){
                dailyData.put(newDate, new LinkedHashMap<>());
            }
            dailyData.get(newDate).put(dateString, allData.get(dateString));
        }
        return dailyData;
    }

    public static LinkedHashMap<DayOfWeek, LinkedHashMap<String, ArrayList<Double>>> calculateDayOfWeekData(LinkedHashMap<String, ArrayList<Double>> allData){
        LinkedHashMap<DayOfWeek, LinkedHashMap<String, ArrayList<Double>>> dayOfWeekData = new LinkedHashMap<>();
        for (String dateString: allData.keySet()){
            LocalDateTime date = LocalDateTime.parse(dateString, formatter);
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            if (!dayOfWeekData.containsKey(dayOfWeek)){
                dayOfWeekData.put(dayOfWeek, new LinkedHashMap<>());
            }
            dayOfWeekData.get(dayOfWeek).put(dateString, allData.get(dateString));
        }
        return dayOfWeekData;
    }

    public static LinkedHashMap<Integer, LinkedHashMap<String, ArrayList<Double>>> calculateYearlyData(LinkedHashMap<String, ArrayList<Double>> allData){
        LinkedHashMap<Integer, LinkedHashMap<String, ArrayList<Double>>> yearlyData = new LinkedHashMap<>();
        for (String dateString: allData.keySet()){
            LocalDateTime date = LocalDateTime.parse(dateString, formatter);
            int year = date.getYear();
            if (!yearlyData.containsKey(year)){
                yearlyData.put(year, new LinkedHashMap<>());
            }
            yearlyData.get(year).put(dateString, allData.get(dateString));
        }
        return yearlyData;
    }

    public static void calculateMonthlyData(Worksheet worksheet, ArrayList<String> names, LinkedHashMap<String, ArrayList<Double>> allData, int srow, int scol){
        LinkedHashMap<Month, LinkedHashMap<String, ArrayList<Double>>> monthlyData = new LinkedHashMap<>();
        for (String dateString: allData.keySet()){
            LocalDateTime date = LocalDateTime.parse(dateString, formatter);
            Month month = Month.of(date.getMonthValue());
            if (!monthlyData.containsKey(month)){
                monthlyData.put(month, new LinkedHashMap<>());
            }
            monthlyData.get(month).put(dateString, allData.get(dateString));
        }

        Cells cells = worksheet.getCells();
        int coolingEnergyIndex = names.size();
        int heatingEnergyIndex = names.size()+1;
        cells.get(srow, scol).setValue("Month");
        for (int i = 0; i< names.size(); i++){
            cells.get(srow, scol+i+1).setValue(names.get(i));
        }
        cells.get(srow, scol+coolingEnergyIndex+1).setValue("Cooling Energy (thousand Btu)");
        cells.get(srow, scol+heatingEnergyIndex+1).setValue("Heating Energy (thousand Btu)");

        int row = srow+1;

        for (Month time : monthlyData.keySet()){
            cells.get(row, scol).setValue(time.toString());
            LinkedHashMap<String, ArrayList<Double>> keyDate_values = monthlyData.get(time);
            for (int i = 0; i <= heatingEnergyIndex; i++){

                boolean isTemperature = false;
                if (i < names.size()){
                    isTemperature = names.get(i).contains("℉");
                }
                Double result = 0.0;
                if (isTemperature){
                    int n = keyDate_values.keySet().size();
                    Double total = 0.0;
                    for (String date: keyDate_values.keySet()){
                        total += keyDate_values.get(date).get(i);
                    }
                    result = total / n;
                } else {
                    Double total = 0.0;
                    for (String date: keyDate_values.keySet()){
                        total += keyDate_values.get(date).get(i);
                    }
                    result = total;
                }
                cells.get(row, scol+i+1).setValue(result);
            }
            row++;
        }
    }

    public static void main(String[] args){
//        String filename = System.getProperty("user.dir") + "/src/OCCUPANCY.xlsx";
//        saveReportsFromWebCtrlToExcel(filename, "/trees/geographic/rochester_campus/building_70", "~effective-schedule");
        System.out.println(getReportsFromWebCtrl("/trees/geographic/rochester_campus/building_70", "~effective-schedule"));
    }
}
