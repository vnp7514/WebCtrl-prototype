import com.aspose.cells.Cells;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Scanner;

public class APICALL {
    private class Data {
        private double WSF2;
        private String STATION;
        private double WSF5;
        private double SNOW;
        private double PRCP;
        private double SNWD;
        private String DATE;
        private double WDF2;
        private double AWND;
        private double WDF5;
        private double WT01;
        private double TMAX;
        private double WT02;
        private double TAVG;
        private double TMIN;
    }

    /**
     * Getting NOAA dataset about degree days
     * @param startDate format: 2022-01-01
     * @param endDate format: 2022-12-31
     * @return
     */
    public static HashMap<String, Double> retrieveData(String startDate, String endDate){
        try{
            String urlString = "https://www.ncei.noaa.gov/access/services/data/v1?" +
                    "dataset=daily-summaries&stations=USW00014768&startDate="+
                    startDate+"&endDate="+endDate+"&format=json&units=standard";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();
            int responsecode = conn.getResponseCode();
            if (responsecode != 200) {
                throw new RuntimeException("HttpResponseCode: " + responsecode);
            } else {

                String inline = "";
                Scanner scanner = new Scanner(url.openStream());

                //Write all the JSON data into a string using a scanner
                while (scanner.hasNext()) {
                    inline += scanner.nextLine();
                }

                //Close the scanner
                scanner.close();

                // Deserialization
                Gson gson = new Gson();
                Type collectionType = new TypeToken<Collection<Data>>(){}.getType();
                Collection<Data> data = gson.fromJson(inline, collectionType);

                HashMap<String, Double> keyDate_valueTAVG = new HashMap<>();
                for (Data datum : data){
                    keyDate_valueTAVG.put(datum.DATE, datum.TAVG);
                }
                return keyDate_valueTAVG;
            }
        } catch (Exception e){
            System.err.println(e.getMessage());
        }
        return null;
    }

    public static void saveToExcel(String fileName, HashMap<String, Double> keyDate_valueTAVG){
        try{
            Workbook workbook = new Workbook(fileName);
            Worksheet worksheet = workbook.getWorksheets().add("Degree Days");
            Cells cells = worksheet.getCells();
            cells.get(0,0).setValue("Date");
            cells.get(0,1).setValue("Average Temperature (\u00B0F)");
            cells.get(0,2).setValue("HDD (\u00B0F.day)");
            cells.get(0,3).setValue("CDD (\u00B0F.day)");
            int row = 1;
            double[] monthHDD = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
            double[] monthCDD = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
            for (String date: keyDate_valueTAVG.keySet()){
                cells.get(row, 0).setValue(date);
                Double averageTemp = keyDate_valueTAVG.get(date);
                Double currentDayCDD = averageTemp<=65.0 ? 0 : averageTemp - 65.0;
                Double currentDayHDD = averageTemp>65.0 ? 0 : 65.0 - averageTemp;
                cells.get(row, 1).setValue(averageTemp);
                cells.get(row,2).setValue(currentDayHDD);
                cells.get(row, 3).setValue(currentDayCDD);
                LocalDate d = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
                monthHDD[d.getMonthValue()-1] += currentDayHDD;
                monthCDD[d.getMonthValue()-1] += currentDayCDD;

                row++;
            }

            Worksheet worksheet1 = workbook.getWorksheets().add("Month Degree Days");
            Cells cells1 = worksheet1.getCells();
            cells1.get(0,0).setValue("Month");
            cells1.get(0,1).setValue("Month HDD (\u00B0F.day/month)");
            cells1.get(0,2).setValue("Month CDD (\u00B0F.day/month)");

            for (int i = 1; i <=12; i++){
                cells1.get(i,0).setValue(i);
                cells1.get(i,1).setValue(monthHDD[i-1]);
                cells1.get(i,2).setValue(monthCDD[i-1]);
            }
            workbook.save(fileName);
        } catch (Exception e){
            System.err.println("Error in save to Excel "+ e.getMessage());
        }
    }

    public static void main(String args[]){
        //HashMap<String, Double> result = retrieveData();
        //saveToExcel(System.getProperty("user.dir") + "/src/70-AHU-03-TEST.xlsx", result);
    }
}
