import com.aspose.cells.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class line_chart {

    /**
     * Parse the string to obtain the slope and intercept of the trendline function
     * Example string for equation:
     * "y = -271.93x-15076\nR2 = 0.6547"
     * @param input the string to parse
     */
    public static ArrayList<Double> getInterceptAndSlope(String input){
        Pattern p = Pattern.compile("(y\\s*=\\s*-?[0-9]*\\.?[0-9]*x\\s*(-|\\+)\\s*[0-9]*\\.?[0-9]*)\\n.*");
        Matcher m = p.matcher(input);
        m.matches();
        String equation = m.group(1);
        System.out.println("raw: "+ input + "\n---------------------------------------------------------------------\n");
        System.out.println("equation: "+ equation);
        Pattern p2 = Pattern.compile("y\\s*=\\s*(-?[0-9]*\\.?[0-9]*)x\\s*((-|\\+)\\s*[0-9]*\\.?[0-9]*)");
        Matcher m2 = p2.matcher(equation);
        m2.matches();
        Double slope = Double.parseDouble(m2.group(1).replaceAll("\\s", ""));
        Double intercept = Double.parseDouble(m2.group(2).replaceAll("\\s", ""));
        System.out.println("slope: "+ slope);
        System.out.println("intercept: "+ intercept);
        ArrayList<Double> results = new ArrayList<>(Arrays.asList(slope, intercept));
        return results;
    }

    /**
     * Obtain the key-value pair where key is the month value (int) of the year and
     * value is a list of 2 values: [CDD, Cooling energy]
     * @param fileName the excel file where the data are stored
     * @return a hashmap of key-value pairs described above
     */
    public static HashMap<Integer, ArrayList<Double>> getMonthCDDCoolingEnergy(String fileName){
        try{
            Workbook workbook = new Workbook(fileName);
            Worksheet worksheet = workbook.getWorksheets().get("Month Degree Days");
            Cells cells = worksheet.getCells();
            HashMap<Integer, ArrayList<Double>> keyMonth_ValueCDDCooling = new HashMap<>();
            for (int i = 1; i <= 12; i++){
                ArrayList<Double> CDDCooling = new ArrayList<>();
                CDDCooling.add(cells.get(i,2).getDoubleValue());
                CDDCooling.add(cells.get(i,4).getDoubleValue());
                Integer month = cells.get(i, 0).getIntValue();
                keyMonth_ValueCDDCooling.put(month, CDDCooling);
            }
            return keyMonth_ValueCDDCooling;
        } catch (Exception e){
            System.err.println("getMonthCDDCoolingEnergy error" + e.getMessage());
        }
        return null;
    }

    /**
     * Obtain the key-value pair where key is the month value (int) of the year and
     * value is a list of 2 values: [HDD, Heating energy]
     * @param fileName the excel file where the data are stored
     * @return a hashmap of key-value pairs described above
     */
    public static HashMap<Integer, ArrayList<Double>> getMonthHDDHeatingEnergy(String fileName){
        try{
            Workbook workbook = new Workbook(fileName);
            Worksheet worksheet = workbook.getWorksheets().get("Month Degree Days");
            Cells cells = worksheet.getCells();
            HashMap<Integer, ArrayList<Double>> keyMonth_ValueHDDHeating = new HashMap<>();
            for (int i = 1; i <= 12; i++){
                ArrayList<Double> HDDHeating = new ArrayList<>();
                HDDHeating.add(cells.get(i,1).getDoubleValue());
                HDDHeating.add(cells.get(i,3).getDoubleValue());
                Integer month = cells.get(i,0).getIntValue();
                keyMonth_ValueHDDHeating.put(month, HDDHeating);
            }
            return keyMonth_ValueHDDHeating;
        } catch (Exception e){
            System.err.println("getMonthHDDHeatingEnergy error" + e.getMessage());
        }
        return null;
    }

    public static HashMap<String, ArrayList<Double>> graphBaseLine(String fileName) {
        try {
            HashMap<String, ArrayList<Double>> slopeIntercepts = new HashMap<>();
            Workbook workbook = new Workbook(fileName);
            Worksheet worksheet = workbook.getWorksheets().get("Month Degree Days");
            int chartIndex = worksheet.getCharts().add(ChartType.SCATTER, 10, 10, 20, 20);
            Chart chart = worksheet.getCharts().get(chartIndex);
            chart.getTitle().setText("CDD vs Cooling Energy");
            chart.getCategoryAxis().getTitle().setText("CDD (\u00B0F.day/month)");
            chart.getValueAxis().getTitle().setText("Cooling Energy (thousand Btu)");
            //Adding NSeries (chart data source) to the chart
            chart.getNSeries().add("E2:E13", true);
            chart.getNSeries().get(0).setName("Cooling Energy");
            //Setting the data source for the category data of NSeries
            chart.getNSeries().setCategoryData("C2:C13");
            //adding a linear trendline
            int index = chart.getNSeries().get(0).getTrendLines().add(TrendlineType.LINEAR);
            Trendline trendline = chart.getNSeries().get(0).getTrendLines().get(index);
            //Setting the custom name of the trendline.
            trendline.setName("CDD vs Cooling Energy");
            //Displaying the equation on chart
            trendline.setDisplayEquation(true);
            //Displaying the R-Squared value on chart
            trendline.setDisplayRSquared(true);
            chart.calculate();
            slopeIntercepts.put("Cooling", getInterceptAndSlope(trendline.getDataLabels().getText()));


            int chartIndex2 = worksheet.getCharts().add(ChartType.SCATTER, 10, 10, 20, 20);
            Chart chart2 = worksheet.getCharts().get(chartIndex2);
            chart2.getTitle().setText("HDD vs Heating Energy");
            chart2.getCategoryAxis().getTitle().setText("HDD (\u00B0F.day/month)");
            chart2.getValueAxis().getTitle().setText("Heating Energy (thousand Btu)");
            //Adding NSeries (chart data source) to the chart
            chart2.getNSeries().add("D2:D13", true);
            chart2.getNSeries().get(0).setName("Heating Energy");
            //Setting the data source for the category data of NSeries
            chart2.getNSeries().setCategoryData("B2:B13");
            //adding a linear trendline
            int index2 = chart2.getNSeries().get(0).getTrendLines().add(TrendlineType.LINEAR);
            Trendline trendline2 = chart2.getNSeries().get(0).getTrendLines().get(index2);
            //Setting the custom name of the trendline.
            trendline2.setName("HDD vs Heating Energy");
            //Displaying the equation on chart
            trendline2.setDisplayEquation(true);
            //Displaying the R-Squared value on chart
            trendline2.setDisplayRSquared(true);
            chart2.calculate();
            slopeIntercepts.put("Heating", getInterceptAndSlope(trendline2.getDataLabels().getText()));


            //Saving the Excel file
            workbook.save(fileName);
            return slopeIntercepts;
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    public static void main(String[] args) {
        String filename = System.getProperty("user.dir") + "/src/2021.xlsx";
        HashMap<String, ArrayList<Double>> slopeIntercepts = graphBaseLine(filename);
        makeBaseLineSheet(filename, slopeIntercepts);
    }

    /**
     * Make both the cooling energy sheet and the heating energy sheet
     * @param fileName the excel file where both sheets will be stored as well as where the data are
     * @param slopeIntercepts the slope and intercepts of both trendLines
     *                        (CDD vs cooling energy, and HDD vs heating energy)
     */
    public static void makeBaseLineSheet(String fileName, HashMap<String, ArrayList<Double>> slopeIntercepts){
        try{
            ArrayList<Double> cooling = slopeIntercepts.get("Cooling");
            System.out.println("Cooling slope: "+ cooling.get(0));
            System.out.println("Cooling intercept: "+ cooling.get(1));
            ArrayList<Double> heating = slopeIntercepts.get("Heating");
            System.out.println("Heating slope: "+ heating.get(0));
            System.out.println("Heating intercept: "+ heating.get(1));
            makeCoolingBaseLineSheet(fileName, cooling.get(1), cooling.get(0));
            makeHeatingBaseLineSheet(fileName, heating.get(1), heating.get(0));


        } catch (Exception e) {
            System.err.println("makeBaseLineSheet error" + e.getMessage());
        }
    }

    /**
     * Make the cooling energy sheet
     * @param fileName the excel file where the data are stored
     * @param intercept the intercept of the trendline from CDD vs cooling energy chart
     * @param slope the slope of the trendline
     */
    public static void makeCoolingBaseLineSheet(String fileName, Double intercept, Double slope){
        try{
            Workbook workbook = new Workbook(fileName);

            Worksheet worksheet = workbook.getWorksheets().add("Cooling BaseLine Info");
            Cells cells = worksheet.getCells();

            cells.get(0,0).setValue("Month");
            cells.get(0,1).setValue("CDD (\u00B0F.day/month)");
            cells.get(0,2).setValue("Intercept");
            cells.get(0,3).setValue("Slope");
            cells.get(0, 4).setValue("Actual Consumption (thousand Btu)");
            HashMap<Integer, ArrayList<Double>> keyMonth_ValueCDDCooling = getMonthCDDCoolingEnergy(fileName);
            for (Integer key: keyMonth_ValueCDDCooling.keySet()){
                Double CDD = keyMonth_ValueCDDCooling.get(key).get(0);
                Double cooling = keyMonth_ValueCDDCooling.get(key).get(1);
                cells.get(key, 0).setValue(key);
                cells.get(key, 1).setValue(CDD);
                cells.get(key, 2).setValue(intercept);
                cells.get(key, 3).setValue(slope);
                cells.get(key, 4).setValue(cooling);
            }
            workbook.save(fileName);

        } catch (Exception e) {
            System.err.println("makeCoolingBaseLineSheet error" + e.getMessage());
        }
    }

    /**
     * Make the heating energy sheet
     * @param fileName the excel file where the data are stored
     * @param intercept the intercept of the trendline from HDD vs heating energy chart
     * @param slope the slope of the trendline
     */
    public static void makeHeatingBaseLineSheet(String fileName, Double intercept, Double slope){
        try{
            Workbook workbook = new Workbook(fileName);

            Worksheet worksheet = workbook.getWorksheets().add("Heating BaseLine Info");
            Cells cells = worksheet.getCells();

            cells.get(0,0).setValue("Month");
            cells.get(0,1).setValue("HDD (\u00B0F.day/month)");
            cells.get(0,2).setValue("Intercept");
            cells.get(0,3).setValue("Slope");
            cells.get(0, 4).setValue("Actual Consumption (thousand Btu)");
            HashMap<Integer, ArrayList<Double>> keyMonth_ValueHDDHeating = getMonthHDDHeatingEnergy(fileName);
            for (Integer key: keyMonth_ValueHDDHeating.keySet()){
                Double HDD = keyMonth_ValueHDDHeating.get(key).get(0);
                Double heating = keyMonth_ValueHDDHeating.get(key).get(1);
                cells.get(key, 0).setValue(key);
                cells.get(key, 1).setValue(HDD);
                cells.get(key, 2).setValue(intercept);
                cells.get(key, 3).setValue(slope);
                cells.get(key, 4).setValue(heating);
            }
            workbook.save(fileName);

        } catch (Exception e) {
            System.err.println("makeHeatingBaseLineSheet error" + e.getMessage());
        }
    }
}
