# WebCtrl-prototype
## Description
This is a prototype to create a baseline graph from existing data. The data is obtained from WebCtrl and NOAA database.

## Code Dependencies
1. Aspose Cells for Java. https://docs.aspose.com/cells/java/
2. GSON. https://github.com/google/gson
3. dotenv-java. https://github.com/cdimascio/dotenv-java
4. NOAA API links.  https://www.ncei.noaa.gov/support/access-data-service-api-user-documentation
5. List of stations for NOAA API: https://www1.ncdc.noaa.gov/pub/data/ghcn/daily/ghcnd-stations.txt
6. Detailed documentation on NOAA API link: https://www.ncei.noaa.gov/pub/data/cdo/documentation/GHCND_documentation.pdf

## How to use
1. Setting up environment variables to login to webctrl. Create a file named `.env` in the project directory with these values: 

```
USER=<User name to login>
PASS=<Password>
```

2. Creating and Initializing the baseline Excel file.
Create an Excel with the following values in a Sheet named: `Trend`

![Example](./images/example.png)

The column `Type` contains the names of the data. This name will appear on other sheets to indicate where the data is coming from. The names provided in the example are must-have name. Without these, the code will not work.

The column `Path` contains the path to the data on WebCtrl.

The column `Start Time` contains the start times.

The column `End Time` contains the end times.

The column `Limit from Start` contains boolean values. If it is true, the number of data retrieved will be equal to the number specified in the next column `max Records` and the data will be retrieved from the start time. 
Otherwise, it will return data from end time to start time.

The column `max Records` contains the number of records to retrieve. If it is `0`, that means retrieve all data.

4. Creating and Initializing the Excel file to be compared. This is the same as the previous step.

5. Setting values for `src/Main.java` so we can start the program. 

`baseLine` is the baseline Excel

`compare` is the comparing Excel

`output` is where the baseline should be output to.



# Explanations
- **\% -> Anything with an asterisk before it means that it has to be included in the final MVP. It is not in the prototype.**
## Step 1: Pulling data from NOAA
This is the API URL that the code will mainly use to pull data from NOAA: 

`https://www.ncei.noaa.gov/access/services/data/v1?dataset=daily-summaries&stations=<station>&startDate=<startDate>&endDate=<endDate>&format=json&units=standard`

This URL pulls data from a dataset called `daily summaries` from NOAA. The code will get the values for `station`, `start date` and `end date`
from the worksheet `Input for Degree Days`. The data will contain various information about all days during the specfied period. The code will extract the 
`average temperature` of each day and use it to calculate `heating degree days (HDD)` and `cooling degree days (CDD)`. These are 
saved in `Degree Days` worksheet under the same names. Then the code will sum up all the degree days to calculate for month-related data. 
The degree days for each month will be stored under `Month Degree Days` worksheet. 

The equation to calculate degree days for each day:
* _Heating degree days_ (°F.day) = if _average temperature_ > 65°F, then 0, else 65°F - _average temperature_
* _Cooling degree days_ (°F.day) = if _average temperature_ <= 65°F, then 0, else _average temperature_ - 65°F

- \% - The baseline degree that is being used in the above equation (65°F) can be changed by the user.
- \% - The user can choose between NOAA data and WebCtrl data for calculating degree days. 

The equation to calculate degree days for each month:
* Heating degree days (°F.day/month) = sum of all heating degree days of all days in that month
* Cooling degree days (°F.day/month) = sum of all cooling degree days of all days in that month

## Step 2: Pulling data from WebCtrl
The following are the must-have names under the `Type` column of `Trend` worksheet in both `baseLine` and `compare` workbooks:
* "070_ahu_03_ma_temp (℉)"
* "070_ahu_03_sa_temp (℉)"
* "070_ahu_03_sa_air_flow (cfm)"
* "oa_temp  (℉)"
* "Preheat Discharge Temp (℉)"

**_These will be used in the Cooling and Heating Calculation_**

Then the code will use the provided information to pull data from WebCtrl. The results will be stored in 2 worksheets: `Trend Values` and `Trend Values Sorted`.
`Trend Value` contains the raw data returned from WebCtrl. For `Trend Values Sorted`, data is aggregated into the same 
time bucket. For example, all data for _**01/01/2019 08:00:00 AM**_ will be under the same bucket. For any trend data
that does not have data for that time, there will be an empty string inserted there.

## Step 3: Calculate Energy
### Cooling and Heating
1) Obtain the entering temperature (Ti) and leaving temperature (To) for cooling and heating calculation for each AHU. Basically, there are 5 user inputs for each AHU:
   1) Cooling Ti
   2) Cooling To
   3) Heating Ti
   4) Heating To
   5) Airflow
   6) Cooling valve
   7) Heating valve
   - **Note**: The user will provide these inputs based on the location of temperature sensor
   - **Energy Engineer Note:** Entering temperature is before the coil, leaving temperature is after the coil.
   - Example for AHU-03:
     - For heating
       - Ti = Mixed Air Temperature (from "070_ahu_03_ma_temp (℉)" trend above)
       - To = Preheat Discharge Temperature (from "Preheat Discharge Temp (℉)" trend above)
       - Heating valve = hot water valve
     - For cooling
       - Ti = Preheat Discharge Temperature (from "Preheat Discharge Temp (℉)" trend above)
       - To = Supply Air Temperature (from "070_ahu_03_sa_temp (℉)" trend above)
       - Cooling valve = chilled water valve
     - Airflow = Supply Airflow ( from "070_ahu_03_sa_air_flow (cfm)" trend above)
   - **The prototype is using the above values by default. For the MVP, it should allow the user to provide and choose the values.**
2) Calculate Q values for Cooling and Heating ( units are in brackets [ ] ) (stored in `Energy` worksheet):
    - General formula for Q values:
      - **Q [Btu/min] = IF valve [%open]> 0 THEN max/min(0.01791 [Btu/(ft^3 . °F)] * Airflow [ft^3/min] * (To [℉] - Ti [℉]), 0) 
      ELSE 0**
    - Example:
      - Apply the general formula to get Cooling Q value for AHU-03:
        - Q Cooling [Btu/min] = IF chilled water valve > 0 THEN MINIMUM( 0, (0.01791  * Supply Airflow * ( Supply Air Temperature - Preheat Discharge Temperature )) ) ELSE 0
      - Apply the general formula to get Heating Q value for AHU-03:
        - Q Heating [Btu/min] = IF hot water valve > 0 THEN MAXIMUM( 0, (0.01791 * Supply Airflow * ( Preheat Discharge Temperature - Mixed Air Temperature )) ) ELSE 0
3) Determine the time interval for energy calculation
   - For the MVP, ideally the time interval will be the time interval in the database for all energy variables specified in step 1  
   - In the prototype code, we are getting the first and second time buckets from Trend Values Sorted worksheet, then use the difference between those as the time interval
    because the data in the worksheet is sorted and any time bucket that is missing data is at the bottom.
4) Calculate Cooling and Heating Energy [Btu] ( units are in brackets [ ] ) (stored in `Energy` worksheet):
   - General formula:
     - **Energy [Btu] = Q [Btu/min] * time interval [min]**
   - Example:
     - Apply the general formula to get Cooling Energy [thousand Btu]:
       - Cooling Energy [thousand Btu] = Q Cooling [Btu/min] * time interval [min] / 1000
       - _Above has /1000 to get thousand Btu, without it, the unit will be Btu_
     - Apply the general formula to get Heating Energy [thousand Btu]:
       - Heating Energy [thousand Btu] = Q Heating [Btu/min] * time interval [min] / 1000
       - _Above has /1000 to get thousand Btu, without it, the unit will be Btu_
     - **Note**: The values above are also stored in `Degree Days` worksheet
5) Determine Occupied and Unoccupied periods
6) Determine the baseline
   1) Energy vs Outdoor Air Temperature
   2) Energy vs Cooling/Heating Degree Days
   3) 
7) Aggregate data under each month
   - The result is saved in the `Month Degree Days` worksheet
   - Calculate the sum of all Cooling energy under a month. For example, calculate
   the cooling energy of January by summing up all cooling energy under all days of January
   - Calculate the sum of all Heatin energy under a month. For example, calculate
   the heating energy of January by summing up all heating energy under all days January
8) Graphing
   1) Cooling Degree Days vs Cooling Energy (For both day data (`Degree Days` worksheet) and 
   month data (`Month Degree Days` worksheet))
      - graph a scatter plot between Cooling Degree Days and Cooling energy
      - X Axis = Cooling Degree Days
      - Y Axis = Cooling Energy
   2) Do the same for Heating Degree Days and Heating Energy
   3) Extract the trendlines from both cooling and heating graphs
9) Create baseline table for cooling and heating
   - Obtain the information below:
      - From the `baseline` Excel graph:
        - the intercept and slope of the heating/cooling month graph above
          - Details:
            - Column `Intercept`, and `Slope` from `Cooling BaseLine Info`. These are obtained from the _Cooling Degree Days vs Cooling Energy_ graph for months
            - Column `Intercept`, and `Slope` from `Heating BaseLine Info`. These are obtained from the _Heating Degree Days vs Heating Energy_ graph for months
      - From the `compare` Excel graph:
        - the actual Cooling degree days and Heating degree days of each month 
          - Details: from `Month Degree Days` worksheet
        - the actual Cooling energy and Heating energy of each month (called this `Actual Consumption`)
            - Details: from `Month Degree Days` worksheet
   - Create the Cooling Baseline table
     - `CDD` is from the `compare` Excel
     - `Intercept` and `Slope` is from the `baseline` Excel
     - `Actual Consumption` is from the `compare` Excel
     - `Adjusted Consumption` = `CDD` * `Slope` + `Intercept`
     - `Savings` = absolute value of the difference between `Actual Consumption` and `Adjusted Consumption`

### Electricity
### Economizer Mode
### Occupied and Unoccupied Consumption