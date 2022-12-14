import com.aspose.cells.Cell;
import com.aspose.cells.Cells;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class Baseline {

    public static void writeBaseline(Worksheet worksheet, String baselineName, ArrayList<String> names, ArrayList<Double> interceptSlope, LinkedHashMap<String, ArrayList<Double>> keyDate_valueDDEnergy, int srow, int scol){
        int row = srow;
        int col = scol;
        Cells cells = worksheet.getCells();
        cells.get(row, col).setValue(baselineName);
        row++;
        for (int i = 0; i < names.size(); i++){
            cells.get(row, col+i).setValue(names.get(i));
        }
        row++;
        Double intercept = interceptSlope.get(0);
        Double slope = interceptSlope.get(1);
        for (String date: keyDate_valueDDEnergy.keySet()){
            Double degreeDays = keyDate_valueDDEnergy.get(date).get(0);
            Double energy = keyDate_valueDDEnergy.get(date).get(1);
            cells.get(row, col).setValue(date);
            cells.get(row,col+1).setValue(degreeDays);
            cells.get(row, col+2).setValue(intercept);
            cells.get(row, col+3).setValue(slope);
            cells.get(row, col+4).setValue(energy);
            cells.get(row, col+5).setValue(degreeDays*slope + intercept);
            cells.get(row, col+6).setValue(Math.abs(degreeDays*slope + intercept - energy));
            row++;
        }
    }

    public static void calculateBaseline(Workbook baseline, Workbook prediction, Workbook output){
        try{
            LinkedHashMap<String, ArrayList<Double>> keyString_valueInterceptSlope = getSlopeIntercept(baseline);
            LinkedHashMap<String, ArrayList<Double>> keyString_valueEnergy = getEnergy(prediction);
            LinkedHashMap<String, ArrayList<Double>> keyString_valueDD = getCDD(prediction);
            Worksheet worksheet = util.getWorksheetFromWorkbook(output,"Cooling");
            Cells cells = worksheet.getCells();
            cells.get(0,0).setValue("Month");
            cells.get(0,1).setValue("CDD (\u00B0F.day/month)");
            cells.get(0,2).setValue("Intercept");
            cells.get(0,3).setValue("Slope");
            cells.get(0,4).setValue("Adjusted Consumption (thousand Btu)");
            cells.get(0, 5).setValue("Actual Consumption (thousand Btu)");
            cells.get(0, 6).setValue("Savings");
            for (int i = 0; i < 12; i++){
                Double actual = keyString_valueEnergy.get("Cooling").get(i);
                Double slope = keyString_valueInterceptSlope.get("Cooling").get(1);
                Double intercept = keyString_valueInterceptSlope.get("Cooling").get(0);
                Double CDD = keyString_valueDD.get("Cooling").get(i);
                cells.get(i+1,0).setValue(i+1);
                cells.get(i+1,1).setValue(CDD);
                cells.get(i+1,2).setValue(intercept);
                cells.get(i+1,3).setValue(slope);
                cells.get(i+1, 4).setValue(CDD*slope+intercept);
                cells.get(i+1,5).setValue(actual);
                cells.get(i+1,6).setValue(Math.abs((slope*CDD + intercept) - actual));
            }

            Worksheet worksheet2 = util.getWorksheetFromWorkbook(output,"Heating");
            Cells cells2 = worksheet2.getCells();
            cells2.get(0,0).setValue("Month");
            cells2.get(0,1).setValue("HDD (\u00B0F.day/month)");
            cells2.get(0,2).setValue("Intercept");
            cells2.get(0,3).setValue("Slope");
            cells2.get(0,4).setValue("Adjusted Consumption (thousand Btu)");
            cells2.get(0, 5).setValue("Actual Consumption (thousand Btu)");
            cells2.get(0, 6).setValue("Savings");
            for (int i = 0; i < 12; i++){
                Double actual = keyString_valueEnergy.get("Heating").get(i);
                Double slope = keyString_valueInterceptSlope.get("Heating").get(1);
                Double intercept = keyString_valueInterceptSlope.get("Heating").get(0);
                Double HDD = keyString_valueDD.get("Heating").get(i);
                cells2.get(i+1,0).setValue(i+1);
                cells2.get(i+1,1).setValue(HDD);
                cells2.get(i+1,2).setValue(intercept);
                cells2.get(i+1,3).setValue(slope);
                cells2.get(i+1, 4).setValue(HDD*slope+intercept);
                cells2.get(i+1,5).setValue(actual);
                cells2.get(i+1,6).setValue(Math.abs((slope*HDD + intercept) - actual));
            }

        } catch (Exception e) {
            System.err.println("calculateBaseline error");
            System.err.println(e.getMessage());
        }
    }

    public static LinkedHashMap<String, ArrayList<Double>> getSlopeIntercept(Workbook workbook){
        try{
            LinkedHashMap<String, ArrayList<Double>> result = new LinkedHashMap<>();
            Worksheet worksheet = util.getWorksheetFromWorkbook(workbook, "Cooling BaseLine Info");
            Cells cells = worksheet.getCells();
            ArrayList<Double> values = new ArrayList<>();
            values.add(cells.get(1,2).getDoubleValue());
            values.add(cells.get(1,3).getDoubleValue());
            result.put("Cooling", values);
            Worksheet worksheet2 = util.getWorksheetFromWorkbook(workbook,"Heating BaseLine Info");
            Cells cells2 = worksheet2.getCells();
            ArrayList<Double> values2 = new ArrayList<>();
            values2.add(cells2.get(1,2).getDoubleValue());
            values2.add(cells2.get(1,3).getDoubleValue());
            result.put("Heating", values2);
            return result;

        } catch (Exception e) {
            System.err.println("getSlopeIntercept error");
            System.err.println(e.getMessage());
        }
        return null;
    }

    public static LinkedHashMap<String, ArrayList<Double>> getEnergy(Workbook workbook){
        try{
            LinkedHashMap<String, ArrayList<Double>> result = new LinkedHashMap<>();
            Worksheet worksheet = util.getWorksheetFromWorkbook(workbook,"Cooling BaseLine Info");
            Cells cells = worksheet.getCells();
            ArrayList<Double> values = new ArrayList<>();
            for (int i = 1; i <= cells.getMaxDataRow(); i++){
                values.add(cells.get(i, 4).getDoubleValue());
            }
            result.put("Cooling", values);
            Worksheet worksheet2 = util.getWorksheetFromWorkbook(workbook, "Heating BaseLine Info");
            Cells cells2 = worksheet2.getCells();
            ArrayList<Double> values2 = new ArrayList<>();
            for (int i = 1; i <= cells2.getMaxDataRow(); i++){
                values2.add(cells2.get(i, 4).getDoubleValue());
            }
            result.put("Heating", values2);
            return result;

        } catch (Exception e) {
            System.err.println("getEnergy error");
            System.err.println(e.getMessage());
        }
        return null;
    }

    public static LinkedHashMap<String, ArrayList<Double>> getCDD(Workbook workbook){
        try{
            LinkedHashMap<String, ArrayList<Double>> result = new LinkedHashMap<>();
            Worksheet worksheet = util.getWorksheetFromWorkbook(workbook,"Cooling BaseLine Info");
            Cells cells = worksheet.getCells();
            ArrayList<Double> values = new ArrayList<>();
            for (int i = 1; i <= cells.getMaxDataRow(); i++){
                values.add(cells.get(i, 1).getDoubleValue());
            }
            result.put("Cooling", values);
            Worksheet worksheet2 = util.getWorksheetFromWorkbook(workbook,"Heating BaseLine Info");
            Cells cells2 = worksheet2.getCells();
            ArrayList<Double> values2 = new ArrayList<>();
            for (int i = 1; i <= cells2.getMaxDataRow(); i++){
                values2.add(cells2.get(i, 1).getDoubleValue());
            }
            result.put("Heating", values2);
            return result;

        } catch (Exception e) {
            System.err.println("getCDD error");
            System.err.println(e.getMessage());
        }
        return null;
    }

    public static void main(String[] args){
        String baseline = System.getProperty("user.dir") + "/src/70-AHU-03-TEST-2021.xlsx";
        String prediction = System.getProperty("user.dir") + "/src/70-AHU-03-TEST-2022.xlsx";
        String output = System.getProperty("user.dir") + "/src/70-AHU-03-Baseline.xlsx";
    }
}
