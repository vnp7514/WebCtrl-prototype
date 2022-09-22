# Occupancy Profile

- **\% -> Anything with an asterisk before it means that it has to be included in the final MVP. It is not in the prototype.**

- \% The prototype is only focused on 1 room and 1 VAV. **The MVP should work for all rooms, VAVs and AHUs.**

- \% For the MVP, the user should be able to choose a start time and endtime for gathering data

- \% For the MVP, there should be an option to automatically choose a start time and endtime when the user select 
**a semester for gathering data**. The start time and end time of all semesters should be recorded 
somewhere. The user can query this data and get the corresponding times.

- \% For the MVP, there should be an option to **identify all holidays on the graphs**

### Step 1: Gathering data: `OCCUPANCY_Input.xlsx` is the example file
- `SIS data` sheet contains the SIS data about the room
  - Program will get data in these columns:
    - Mtg Start
    - Mtg End
    - Tot Enrl
    - Facil ID
    - Mon
    - Tues
    - Wed
    - Thurs
    - Fri
    - Sat
    - Sun
    - \% Comb Sect
      - *NOTE*: There are cases where lecture classes are joined so both sections have to be combined. For example, the `Tot Enrl` for the classroom would be the sum of both sections.
      This has to be solved in the final solution.
      - Check `Comb Sect` column
      

- `Room Input data0` sheet ( other names are `Space Info`, `Mechanical Data` )
  - Program will get data in these columns:
    - Uncertainty
    - RoomID
    - BuildingID
    - VAV_ID
    - AHU_ID
	

- `WebCtrlReportInput` sheet
  - Contains all the necessary parameters to run the `runReport()` method in WDSL
  - `runReport` will run with CSV as the default value for extension parameter 
  - Program will pull report data from WebCtrl:
    - ~effective-schedule
      - The `~effective-schedule` report is the actual HVAC operational schedule at building level. 
      - \% The protype is currently using the above report from building 70.
		
	


- `WebCtrl Input` sheet
  - Contains all the necessary parameters to run the getTrendData() method
  - Program will get the trend data from WebCtrl:
    - Occupancy Contact State 
      - Occupancy Contact State is showing the status of the occupancy sensor of the VAV
        - \% _There are rooms that have more than 1 VAVs and more than 1 occupancy sensor_. 
        This will have to be evaluated in the final MVP. For example: in building 70 room 
        1550, there are VAV 307 and 309. VAV 307 has an occupancy sensor (denoted by `Y`). 
        VAV 309 does not have its own VAV. It uses the VAV 307's sensor instead (denoted by `Y*`).




### To summarize, there will be 3 sources of data:
- SIS data (Event Schedule)
- Occupancy Sensor Trend data (Actual Space Use)
- Effective Schedule Report data (Equipment Schedule)




### Step 2: Make an occupancy table for each of the 3 sources of data (Example: `OccupancyProfile` worksheet):
- the interval for time is **15 minutes** ( \% can be changed the users)
  - \% For the user interface, the user is able to select the time interval for analysis
- Each row represent all the occupancy values in a day of the week (Mon, Tues and so on)
- Each column represents all the occupancy values at the specified time in the day of the week


- How to determine occupancy values for each of the data sources:
  - **SIS data**: (This table starts from row 1)
    - _occupancy value = Tot Enrl + Uncertainty_
  - **Occupancy Sensor**: (this table starts from row 31)
    - _occupancy value = 0 or 1_
       _where 0 is unoccupied and 1 is occupied_ 
  - **Report**: (this table starts from row 16)
    - _occupancy value = if occupied, then (max occupancy value in SIS data), else 0_

- Graphs:
  - Occupancy Profile for Monday 
  - Occupancy Profile for Tuesday 
  - and so on until Sunday
  
- As for details on graphs that will be made from these tables:
  - Effective Schedule and SIS Data should be plotted on the primary Y axis (Left). 
  - Occupancy Sensor State data should be plotted on the secondary Y axis (Right). 
  - The X axis would be time in a day of the week


### Step 3: Make a continuous occupancy table for each of the data sources (Example: `ContinuousOccupancyProfile` worksheet):
- Basically time includes the day of the week as well
- Graphs:
  - Occupancy Profile for all weekdays (Mon-Fri)
  - Occupancy Profile for all weekends (Sat-Sun)
  - Occupancy Profile for all days 
  - Occupancy Profile for all weeks 
  - Occupancy Profile for all months 
  - Occupancy Profile for all years 
  - Occupancy Profile for all semesters

- As for the graphs that will be made from this table:
  - Effective Schedule and SIS Data should be plotted on the primary Y axis (Left). 
  - Occupancy Sensor State data should be plotted on the secondary Y axis (Right).



### Step 4: Get the total Hours occupied for each day of the week and make a table (Example: `hoursOccupancyProfile` worksheet)
- Graphs:
  - Room hours use per day 
  - Room hours use per week

### Step 5: Providing Recommended Schedule based on SIS Data and Occupancy Sensor status
- Determine the start time and end time of each day (business hours). For example, `3statuses` worksheet. 
  - The start time is the first time when the occupancy value first turns positive (> 0) since 
  00:00 AM (midnight) that day
  - The end time is the last time when the occupancy value is positive (> 0) until 11:59 PM that day
- Add new columns in the Report data from WebCtrl (`ReportRawData` worksheet):
  1) `Recommended Schedule Based on SIS`
     - Convert the calculated start time and end time for each day above into text and add it under this column.
     - The start time and end time should depend on the SIS data.
     - For example:
       - SIS Data for VAV 338:
         - Monday start time: 5:00 AM
         - Monday end time: 6:00 PM
         - Tuesday start time: 11:00 AM
         - Tuesday end time: 6:00 PM
       - In the same row as VAV-338 in `ReportRawData` worksheet, under the `Recommended Schedule Based on SIS`:
         - Put the value: "Monday Occupied from 5:00 AM to 6:00 PM \n
         Tuesday Occupied from 11:00 AM to 6:00 PM" 
  2) `Recommended Schedule Based on Sensor`
     - Perform the same conversion as above and put it under this column
  - **/% For the MVP, this has to be done for all VAVs. In the protoype, only 2 VAVs are done
  as an example**
  - **/% For the MVP, there should be a way to identify holidays in the data/graph**
  - **/% For the MVP, there should be information about the start and end date of a semester available for the user**

### /% Step 6: Calculate occupancy profiles for the AHU based on all occupancy profiles for all VAVs under that AHU
- Create 3 occupancy profiles: (use the occupancy profile from `OccupancyProfile` worksheet 
where all the occupancy values are >= 0)
  - SIS Data
    - The occupancy value = sum of all the occupancy values under the same time and date from all the VAVS under that AHUs
    - For example:
      - AHU 3 has VAV 338 and VAV 337
      - AHU 3 occupancy value at Monday 00:00 AM = VAV 338 occupancy value at Monday 0:00 AM + VAV 337 occupancy value at Monday 0:00 AM 
  - Occupancy Sensor Data
    - Same as SIS Data
  - Report Data
    - Same as SIS Data

### /% Step 7: Export the AHU occupancy profiles to Energy calculation:
  - Assign occupancy values (populations) to each time of energy trend data:
    
  - Assign occupancy status (Occupied or Unoccupied) to each time of energy trend data:
    - Occupancy status is Occupied if the occupancy value is not 0
    - Occupancy status is Unoccupied if the occupancy value is 0



