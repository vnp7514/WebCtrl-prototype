import Eval.EvalServiceLocator;
import Eval.EvalSoapBindingStub;
import ReportWSDL.ReportServiceLocator;
import ReportWSDL.ReportSoapBindingStub;
import SystemWSDL.SystemSoapBindingStub;
import SystemWSDL.SystemapiServiceLocator;
import Trend.TrendServiceLocator;
import Trend.TrendSoapBindingStub;
import com.aspose.cells.*;
import com.opencsv.CSVReader;
import io.github.cdimascio.dotenv.Dotenv;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
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
    private static final String ECONOMIZER = "Economizer";
    private static final String MAT = "070_ahu_03_ma_temp (℉)";
    private static final String SAT = "070_ahu_03_sa_temp (℉)";
    private static final String SACFM = "070_ahu_03_sa_air_flow (cfm)";
    private static final String OAT = "oa_temp  (℉)";
    private static final String PREHEAT = "Preheat Discharge Temp (℉)";
    private static final int dateEnergyCol = 0;
    private static final int EconomizerEnergyCol = 1;
    private static final int OATEnergyCol = 2;
    private static final int MATEnergyCol = 3;
    private static final int SATEnergyCol = 4;
    private static final int PreheatEnergyCol = 5;
    private static final int QCoolingEnergyCol = 6;
    private static final int QHeatingEnergyCol = 7;
    private static final int CoolingEnergyCol = 8;
    private static final int HeatingEnergyCol = 9;
    public static final int SISOccupancyEnergyCol = 10;
    public static final int TrendOccupancyEnergyCol = 11;
    public static final int ReportOccupancyEnergyCol = 12;
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

            LinkedHashMap<String, ArrayList<Double>> result1 = calculateEnergy(workbook, parameters.get(0), sorted);
            // TODO: Ignore for now
            // makeDaySheet(workbook, parameters.get(0), sorted);
            makeMonthEnergySheet(workbook, result1);
            makeDayEnergySheet(workbook, result1);

        } catch (Exception e){
            System.err.println("baseLineTask error "+e.getMessage());
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
     *
     * @param workbook the excel sheet where values will be saved to
     * @param names
     * @param sorted
     * @return key is date. value is a list of [cooling energy (MBtu), qheating (MBtu)]
     */
    private static LinkedHashMap<String, ArrayList<Double>> calculateEnergy(Workbook workbook, ArrayList<String> names, LinkedHashMap<String, ArrayList<String>> sorted){
        try{
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a");
            int econ = 0;
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
                }
            }
            if (econ == mat || econ == sat || econ == sacfm || econ == oat || mat == sat
                    || mat == sacfm || mat == oat || sat == sacfm || sat == oat
                    || sacfm == oat || econ == preheat || mat == preheat || sat == preheat ||
                    sacfm == preheat || oat == preheat) {
                throw new Exception("not enough arguments");
            }


            Worksheet worksheet = getWorksheetFromWorkbook(workbook,"Energy");
            Cells cells = worksheet.getCells();
            // Column index
            int col = 0;
            // Row index
            int row = 0;
            cells.get(row, dateEnergyCol).setValue("Date");
            cells.get(row, EconomizerEnergyCol).setValue("Economizer");
            cells.get(row, OATEnergyCol).setValue("OAT");
            cells.get(row, MATEnergyCol).setValue("MAT");
            cells.get(row, SATEnergyCol).setValue("SAT");
            cells.get(row, PreheatEnergyCol).setValue("Preheat");
            cells.get(row, QCoolingEnergyCol).setValue("QCooling (Btu/min)");
            cells.get(row, QHeatingEnergyCol).setValue("QHeating (Btu/min)");
            cells.get(row, CoolingEnergyCol).setValue("Clg Energy from QCooling (thousand Btu)");
            cells.get(row, HeatingEnergyCol).setValue("Htg Energy from QHeating (thousand Btu)");

            LinkedHashMap<String, ArrayList<Double>> energy = new LinkedHashMap<>();
            row++;
            int initialRow = row;
            // Getting Q values
            LinkedHashMap<String, Double> qCoolingValues = new LinkedHashMap<>();

            LinkedHashMap<String, Double> qHeatingValues = new LinkedHashMap<>();
            for (String date : sorted.keySet()){
                if (sorted.get(date).size() > 6&& !sorted.get(date).get(oat).equalsIgnoreCase("") &&
                        !sorted.get(date).get(mat).equalsIgnoreCase("") &&
                        !sorted.get(date).get(sat).equalsIgnoreCase("") &&
                        !sorted.get(date).get(econ).equalsIgnoreCase("") &&
                        !sorted.get(date).get(sacfm).equalsIgnoreCase("") &&
                        !sorted.get(date).get(preheat).equalsIgnoreCase("")) {
                    ArrayList<String> values = sorted.get(date);
                    qCoolingValues.put(date, 0.01791 * Double.parseDouble(values.get(sacfm)) *
                            (Double.parseDouble(values.get(sat)) - Double.parseDouble(values.get(preheat))));
                    qHeatingValues.put(date, 0.01791 * Double.parseDouble(values.get(sacfm)) *
                            (Double.parseDouble(values.get(preheat)) - Double.parseDouble(values.get(mat))));
                }
            }

            // Getting time interval
            int count = 0;
            Double interval = 0.0;
            for (String date: qCoolingValues.keySet()){
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

            //Writing it to excel
            for (String date: qCoolingValues.keySet()){
                cells.get(row, dateEnergyCol).setValue(date);
                cells.get(row, QCoolingEnergyCol).setValue(qCoolingValues.get(date));
                cells.get(row, QHeatingEnergyCol).setValue(qHeatingValues.get(date));
                cells.get(row, EconomizerEnergyCol).setValue(sorted.get(date).get(econ));
                cells.get(row, OATEnergyCol).setValue(sorted.get(date).get(oat));
                cells.get(row, MATEnergyCol).setValue(sorted.get(date).get(mat));
                cells.get(row, SATEnergyCol).setValue(sorted.get(date).get(sat));
                cells.get(row, PreheatEnergyCol).setValue(sorted.get(date).get(preheat));
                cells.get(row, HeatingEnergyCol).setValue(qHeatingValues.get(date) * interval / 1000);
                cells.get(row, CoolingEnergyCol).setValue(qCoolingValues.get(date) * interval / 1000);
                energy.put(date, new ArrayList<>(Arrays.asList(0.0, 0.0)));
                energy.get(date).add(0, qCoolingValues.get(date) * interval / 1000);
                energy.get(date).add(1, qHeatingValues.get(date) * interval / 1000);
                row++;
            }

            return energy;

        } catch (Exception e){
            System.err.println("calculateEnergy error " + e.getMessage());
        }
        return null;
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

    public static void syncOccupancyTableDataWithEnergyData(Worksheet energyWorksheet, String columnName, TreeMap<Integer, TreeMap<LocalTime, Integer>> ccupancyTablesMapping, int col){
        Cells cells = energyWorksheet.getCells();
        cells.get(0, col).setValue(columnName);
        for (int i = 1; i <= cells.getMaxDataRow(); i++){
            LocalDateTime currentTime = LocalDateTime.parse(cells.get(i, dateEnergyCol).getStringValue(), formatter);
            TreeMap<LocalTime, Integer> currentOccupancyTable = ccupancyTablesMapping.get(currentTime.getDayOfWeek().getValue());
            if (currentOccupancyTable.containsKey(currentTime.toLocalTime())){
                if (currentOccupancyTable.get(currentTime.toLocalTime()) > 0){
                    cells.get(i, col).setValue(1);
                } else {
                    cells.get(i, col).setValue(currentOccupancyTable.get(currentTime.toLocalTime()));
                }
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
            LocalDateTime currentDateTime = LocalDateTime.parse(cells.get(i, dateEnergyCol).getStringValue(), formatter);
            Double currentOAT = Double.parseDouble(cells.get(i, OATEnergyCol).getStringValue());
            Double currentCoolingEnergy = cells.get(i, CoolingEnergyCol).getDoubleValue();
            Double currentHeatingEnergy = cells.get(i, HeatingEnergyCol).getDoubleValue();
            if (!result.containsKey(currentOccupancyValue)){
                result.put(currentOccupancyValue, new TreeMap<>());
            }
            result.get(currentOccupancyValue).put(currentDateTime, new ArrayList<>(Arrays.asList(currentOAT, currentCoolingEnergy, currentHeatingEnergy)));
        }
        return result;
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

    public static void main(String[] args){
//        String filename = System.getProperty("user.dir") + "/src/OCCUPANCY.xlsx";
//        saveReportsFromWebCtrlToExcel(filename, "/trees/geographic/rochester_campus/building_70", "~effective-schedule");
        System.out.println(getReportsFromWebCtrl("/trees/geographic/rochester_campus/building_70", "~effective-schedule"));
    }
}
