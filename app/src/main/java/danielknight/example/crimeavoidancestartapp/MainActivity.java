package danielknight.example.crimeavoidancestartapp;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    //Class Constants
    private static final String packageName = "danielknight.example.googlemapsv5";
    //File used to communicate
    // between both apps. Must have same Name and fixed format(Defined elsewhere)
    private static final String cdDataFile = "crimeData.txt";
    private static final String distanceUnits = "K"; //Kilometers
    private static final String Severity1 = "Severity1";
    private static final String Severity2 = "Severity2";
    private static final String Severity3 = "Severity3";
    private static final String Severity4 = "Severity4";
    private static final String SeverityAll = "All Severity";
    private static final String SeverityInfo = "Please Select a Severity";
    private static String severityName = "";
    private static double cSeverity = 0.0;
    private static final String[] FileSpinnerArray = new String[]{
            "Select a crime data file from the list",
            "_2021_05_Reading.csv", //Sample Data
            "_2021_05_avon_and_somerset_street.csv",
            "_2021_05_bedfordshire_street.csv",
            "_2021_05_btp_street.csv",
            "_2021_05_cambridgeshire_street.csv",
            "_2021_05_cheshire_street.csv",
            "_2021_05_city_of_london_street.csv",
            "_2021_05_cleveland_street.csv",
            "_2021_05_cumbria_street.csv",
            "_2021_05_derbyshire_street.csv",
            "_2021_05_devon_and_cornwall_street.csv",
            "_2021_05_dorset_street.csv",
            "_2021_05_durham_street.csv",
            "_2021_05_dyfed_powys_street.csv",
            "_2021_05_essex_street.csv",
            "_2021_05_gloucestershire_street.csv",
            "_2021_05_gwent_street.csv",
            "_2021_05_hampshire_street.csv",
            "_2021_05_hertfordshire_street.csv",
            "_2021_05_humberside_street.csv",
            "_2021_05_kent_street.csv",
            "_2021_05_lancashire_street.csv",
            "_2021_05_leicestershire_street.csv",
            "_2021_05_lincolnshire_street.csv",
            "_2021_05_merseyside_street.csv",
            "_2021_05_metropolitan_street.csv",
            "_2021_05_norfolk_street.csv",
            "_2021_05_north_wales_street.csv",
            "_2021_05_north_yorkshire_street.csv",
            "_2021_05_northamptonshire_street.csv",
            "_2021_05_northern_ireland_street.csv",
            "_2021_05_northumbria_street.csv",
            "_2021_05_nottinghamshire_street.csv",
            "_2021_05_south_wales_street.csv",
            "_2021_05_south_yorkshire_street.csv",
            "_2021_05_staffordshire_street.csv",
            "_2021_05_suffolk_street.csv",
            "_2021_05_surrey_street.csv",
            "_2021_05_sussex_street.csv",
            "_2021_05_thames_valley_street.csv",
            "_2021_05_warwickshire_street.csv",
            "_2021_05_west_mercia_street.csv",
            "_2021_05_west_yorkshire_street.csv",
            "_2021_05_wiltshire_street.csv",    };

    //Error codes
    private static final int noError = 0;
    private static final int ErrorCode = -1;
    private static final int originError = -100; // the origin mission
    private static final int destinationError = -200; // the destination is missing
    private static final int locationFormatError = -300; // illegal characters in the string (: ;)
    private static final int circleBoundsError = -1001; // Data error - within Circle calculation
    private static final double severityValueError = -1010; //Data error - Severity strings

    //Return codes
    private static final int isNotInCircle = -1000; // given Latlng point not in circle
    // Values assigned to Severities
    // Higher values include all lower values, hence to include all crimes is highest value
    // Severities values could be further categorized if necessary
    // Refer to crimeTypeSeverity()
    // based off:
    // https://www.police.uk/pu/about-police.uk-crime-data/
    //Accessed:
    //  15/07/2021
    // SEE PAGE XX IN DISSERTATION
    private static final double severityValue1 = 1.0; //E.g. "violence and sexual offences"
    private static final double severityValue2 = 2.0; //E.g. Theft
    private static final double severityValue3 = 3.0; //E.g. Anti-social behaviour
    private static final double severityValue4 = 4.0; // Any other crime
    private static final double severityValueAll = 100.0; // All crime
    private static final String[] SeveritySpinnerArray = new String[]{
            SeverityInfo,
            SeverityAll,
            Severity1,
            Severity2,
            Severity3,
            Severity4   };

    //Global Constants
    // incident circle is given radius to include local crime data points
    private static double sizeOfIncidentCircle = 0.0;
    private static int MaxtoRead = 0; //Max number of file lines to read
    private static int readAll = -10; //This must be negative
    public double MaxWeight = 0.0; // highest weighting value for all incident circles
    public static final int maxHeatIncrements = 7; // max HeatMap buckets
    public int HeatMapIncrements = maxHeatIncrements;// number of increments (buckets)

    //Used to restrict the size of the input file (Debugging purposes)
    //Number is number of valid lines to be read.
    private static int tinyData = 100;
    private static int mediumData = 1000;
    private static int largeData = 5000;

    //Class Variables
    //Display Interface controls
    //start and end point needs Full address format
    private EditText mOrigin; // Screen input control for start point
    private EditText mDestination; // Screen input control box for destination
    private String routeOrigin = null; // Start point of journey
    private String routeDestination = null; // End point of the journey

    private Spinner filetypespinner = null; // input control for choosing raw crime data files
    private Spinner severityspinner = null; // control for choosing which severities to include
    private RadioGroup rdgMaxCountGroup = null; // group control for number of lines to read
    private float seekBarPosition = 0.0f; // input control for incident circle size


    //Input/Output buffers
    // location to place strings read from file and to create strings to write to file
    private String bufferString = "";
    private StringBuilder readText; //???

    //Class Complex Variables
    public String HeatMapColorNames[];
    public double HeatMapBreaks[]; //This must be the same as the number of heat map increments
    private ArrayList<wDatapoint> ActiveDataArray = new ArrayList<>(); //Stores relevant data points


    /**
     * ---------------------------------------------------------------------------------------------
     * THIS SECTION IS THE SUBCLASSES
     * ---------------------------------------------------------------------------------------------
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * wDatapoint
     *
     *  SUBCLASS DESCRIPTION
     *      Used to convey information about the crimes and their severities
     *      the weight is updated depending on the number of data points within a
     *      circle(defined by LatLong, Radius)
     *      When a crime is added the severity is increased
     *
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     */
    public static class wDatapoint {
        public Double Lat;
        public Double Lng;
        public Double Wgt;

        /**
         * -------------------------------------------------------------------------------------------------
         * wDatapoint(Double pLat, Double pLng, Double pWgt)
         *
         *  FUNCTION DESCRIPTION
         *      Constructor for the class
         *
         * @author Daniel Knight
         * @version 1.0
         * @since 30/07/2021
         * ---------------------------------------------------
         * @param pLat
         * @param pLng
         * @param pWgt
         */
        public wDatapoint(Double pLat, Double pLng, Double pWgt)
        {
            Lat = pLat;
            Lng = pLng;
            Wgt = pWgt;
        }
        /**
         * * End of wDatapoint()
         * *---------------------------------------------------
         *
         */
    }
    /**
     * * End of wDatapoint Class
     * *---------------------------------------------------
     *
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * THIS SECTION IS THE END OF SUBCLASSES
     * ---------------------------------------------------------------------------------------------
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * THIS SECTION IS THE METHOD FUNCTIONS
     * ---------------------------------------------------------------------------------------------
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * distanceBetween(double lat1, double lon1, double lat2, double lon2, String unit)
     *
     *  See Below
     *
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     */
    /*::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    /*::                                                                         :*/
    /*::  This routine calculates the distance between two points (given the     :*/
    /*::  latitude/longitude of those points). It is being used to calculate     :*/
    /*::  the distance between two locations using GeoDataSource (TM) products   :*/
    /*::                                                                         :*/
    /*::  Definitions:                                                           :*/
    /*::    Southern latitudes are negative, eastern longitudes are positive     :*/
    /*::                                                                         :*/
    /*::  Function parameters:                                                   :*/
    /*::    lat1, lon1 = Latitude and Longitude of point 1 (in decimal degrees)  :*/
    /*::    lat2, lon2 = Latitude and Longitude of point 2 (in decimal degrees)  :*/
    /*::    unit = the unit you desire for results                               :*/
    /*::           where: 'M' is statute miles (default)                         :*/
    /*::                  'K' is kilometers                                      :*/
    /*::                  'N' is nautical miles                                  :*/
    /*::  Worldwide cities and other features databases with latitude longitude  :*/
    /*::  are available at https://www.geodatasource.com                         :*/
    /*::                                                                         :*/
    /*::  For enquiries, please contact sales@geodatasource.com                  :*/
    /*::                                                                         :*/
    /*::  Official Web site: https://www.geodatasource.com                       :*/
    /*::                                                                         :*/
    /*::           GeoDataSource.com (C) All Rights Reserved 2019                :*/
    /*::                                                                         :*/
    /*::   Accessed at: 25/07/2021                                               :*/
    /*::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    private static double distanceBetween(double lat1, double lon1, double lat2, double lon2,
                                          String unit)
    {
        if ((lat1 == lat2) && (lon1 == lon2)) {
            return 0;
        } else {
            double theta = lon1 - lon2;
            double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2))
                    + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                    * Math.cos(Math.toRadians(theta));
            dist = Math.acos(dist);
            dist = Math.toDegrees(dist);
            dist = dist * 60 * 1.1515;
            if (unit.equals("K")) {
                dist = dist * 1.609344;
            } else if (unit.equals("N")) {
                dist = dist * 0.8684;
            }
            return (dist);
        }
    }
    /**
     * * End of distanceBetween()
     * *---------------------------------------------------
     *
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * messageBox()
     *
     *  FUNCTION DESCRIPTION
     *  generic dialog, takes in the method name and error message
     *
     * Reference:
     * https://stackoverflow.com/questions/16561692/android-exception-handling-best-practice
     * @version 1.0
     * Accessed: 30/07/2021
     * ---------------------------------------------------
     * @param method
     * @param message
     */
    private void messageBox(String method, String message)
    {
        Log.d("EXCEPTION: " + method,  message);

        AlertDialog.Builder messageBox = new AlertDialog.Builder(this);
        messageBox.setTitle(method);
        messageBox.setMessage(message);
        messageBox.setCancelable(false);
        messageBox.setNeutralButton("ok", null);
        messageBox.show();
    }
    /**
     * * End of 'messageBox()'
     * *---------------------------------------------------
     *
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * onCreate(Bundle savedInstanceState)
     *
     *  FUNCTION DESCRIPTION
     *      First callback function to be used in the activity lifecycle.
     *      essential functions called.
     *      launch button initialized along with relevant sub parts
     *      Spinners also initialised ??
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Create system Tool bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Creates user input variables
        mOrigin = findViewById(R.id.originTxt);
        mDestination = findViewById(R.id.destinationTxt);
        createSpinners();
        createSeekBars();
        createSeekBars();

        // User control for launching Map app
        Button btnLaunchApp;
        btnLaunchApp = findViewById(R.id.btnStartMaps);
        btnLaunchApp.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                //Process the data
                try {
                    processData();
                    launchApp(packageName);
                    System.exit(noError);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    messageBox("Unable to Start", e.getMessage());
                    System.exit(ErrorCode); //Minus 1 is unable to initialize error
                }
            }
        });
        // Creates Help/Error messages
        loadAppWidgets();
    }
    /**
     * End of onCreate()
     * ----------------------------------------------------
     */

    /**
     * -------------------------------------------------------------------------------------------------
     * loadAppWidgets()
     *
     *  FUNCTION DESCRIPTION
     *      This initialises and sets up the userinface widgets
     *      along with the AlertBoxes that add functionality with help messages
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     */
    private void loadAppWidgets()
    {
        try {
            //-------------------------------------------
            //These are the AlertBoxes
            //------------------------------------------
            final AlertDialog originAlert = new AlertDialog.Builder(
                    MainActivity.this).create();
            originAlert.setTitle("");
            originAlert.setMessage("Please enter the start destination in following the format " +
                    "'Placename,Country");
            originAlert.setButton(AlertDialog.BUTTON_NEUTRAL, "ok",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface originDialog, int i) {
                            originDialog.dismiss();
                        }
                    });

            final AlertDialog destinationAlert = new AlertDialog.Builder(
                    MainActivity.this).create();
            destinationAlert.setTitle("");
            destinationAlert.setMessage("Please enter the end destination in following " +
                    "the format 'Placename,Country");
            destinationAlert.setButton(AlertDialog.BUTTON_NEUTRAL, "ok",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface destinationDialog, int i) {
                            destinationDialog.dismiss();
                        }
                    });

            final AlertDialog filenameAlert = new AlertDialog.Builder(
                    MainActivity.this).create();
            filenameAlert.setTitle("");
            filenameAlert.setMessage("Once a file has been chosen from the selection then"+
                    "it will be passed to GoogleMaps for you navigation needs");
            filenameAlert.setButton(AlertDialog.BUTTON_NEUTRAL, "ok",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface filenameDialog, int i) {
                            filenameDialog.dismiss();
                        }
                    });

            final AlertDialog SeverityAlert = new AlertDialog.Builder(
                    MainActivity.this).create();
            SeverityAlert.setTitle("");
            SeverityAlert.setMessage("The Severity key is as follows:\n" +
                    "\nAll Crime: Lists all crime types \n" +
                    "Severity 1: Violence and Sexual offences \n" +
                    "Severity 2: Vehicle, Weapons, Theft\n" +
                    "Severity 3: Drugs, Criminal Damage, Anti-Social\n" +
                    "Severity 4: Other (E.G. Fraud)");
            SeverityAlert.setButton(AlertDialog.BUTTON_NEUTRAL, "ok",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface severityDialog, int i) {
                            severityDialog.dismiss();
                        }
                    });

            final AlertDialog RadiusBarAlert = new AlertDialog.Builder(
                    MainActivity.this).create();
            RadiusBarAlert.setTitle("");
            RadiusBarAlert.setMessage("Please choose a value between 0 and " +
                    "7.5 for the Size of the Incident Circles");
            RadiusBarAlert.setButton(AlertDialog.BUTTON_NEUTRAL, "ok",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface RadiusBarDialog, int i) {
                            RadiusBarDialog.dismiss();
                        }
                    });

            final AlertDialog loadNumberAlert = new AlertDialog.Builder(
                    MainActivity.this).create();
            loadNumberAlert.setTitle("");
            loadNumberAlert.setMessage("Please choose a value from the selection to load the " +
                    "desired number of valid entries");
            loadNumberAlert.setButton(AlertDialog.BUTTON_NEUTRAL, "ok",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface loadNumberDialog, int i) {
                            loadNumberDialog.dismiss();
                        }
                    });

            //-------------------------------------------
            //These are the img onClickListeners
            //------------------------------------------
            ImageView imgOrigin = findViewById(R.id.picOriginHelp);
            imgOrigin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    originAlert.show();
                }
            });

            ImageView imgDestination = findViewById(R.id.picDestinationHelp);
            imgDestination.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    destinationAlert.show();
                }
            });

            ImageView imgFilename = findViewById(R.id.picFilenameHelp);
            imgFilename.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    filenameAlert.show();
                }
            });

            ImageView imgSeverity = findViewById(R.id.picSeverityHelp);
            imgSeverity.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SeverityAlert.show();
                }
            });

            ImageView imgRadiusSeekBar = findViewById(R.id.picIncrementHelp);
            imgRadiusSeekBar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    RadiusBarAlert.show();
                }
            });

            ImageView imgLoadNumber = findViewById(R.id.picLoadNumberHelp);
            imgLoadNumber.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    loadNumberAlert.show();
                }
            });
        }
        catch(Exception e)
        {
            String currentLine = String.valueOf(getLineNumber());
            messageBox("Initializion Error",
                    "Popup boxes not created" + "Line:" + currentLine);
            e.printStackTrace();
            System.exit(ErrorCode);
        }
    }
    /**
     * * End of loadAppWidgets()
     * *---------------------------------------------------
     *
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * routeNameCheck()
     *
     *  FUNCTION DESCRIPTION
     *      This function will check that the user inputted values are valid
     *      I.E. does string sanitation(removes ';' ect.) and checks
     *
     *  Side Effects
     *      Sets global variables route origin and route destination
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     * @return
     */
    public int routeNameCheck() {

        if ((mOrigin == null) ||
                (mOrigin.getText().toString().equals("Origin"))) { return originError; }

        if ((mDestination == null) ||
                (mDestination.getText().toString().equals("Destination"))) {
            return destinationError; }

        if ((mOrigin.getText().toString().contains(";")) ||
                (mOrigin.getText().toString().contains(":"))) { return locationFormatError; }

        if ((mDestination.getText().toString().contains(";")) ||
                (mDestination.getText().toString().contains(":"))) { return locationFormatError; }

        routeOrigin = mOrigin.getText().toString();
        routeDestination = mDestination.getText().toString();

        return noError;
    }
    /**
     * End of routeNameCheck()
     * ----------------------------------------------------
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * createSeekBars()
     *
     *  FUNCTION DESCRIPTION
     *         This creates the user interface seek bar
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     */
    public void createSeekBars() {
        final TextView progressText; //Text that shows progress value
        SeekBar incidentRadiusBar; // Slider

        incidentRadiusBar = (SeekBar) findViewById(R.id.incidentBar); // set slider to xml
        progressText = (TextView) findViewById(R.id.incidentBarTextProg); // set sliderText to xml

        incidentRadiusBar.setMax(15); // sets max to 7.5 as 7.5 * 2 is 15
        incidentRadiusBar.setProgress(0); //sets minimum

        //on Click changes for slider bar
        try {
            incidentRadiusBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar incidentRadiusBar, int i, boolean b) {
                    seekBarPosition = i / 2.0f; // sets increment to 0.5
                    progressText.setText("" + seekBarPosition + "K"); //Change K to units
                }

                //Functions left in to prevent needing to create complex coding
                //called when user touches the bar
                @Override
                public void onStartTrackingTouch(SeekBar incidentRadiusBar) { }
                //called when the user finishes moving the bar
                @Override
                public void onStopTrackingTouch(SeekBar incidentRadiusBar) { }
            });
        }
        catch(Exception e)
        {
            messageBox("Error", "incident radius bar failed");
            e.printStackTrace();
            System.exit(ErrorCode);
        }
    }
    /**
     * * End of createSeekBars
     * *---------------------------------------------------
     *
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * createSpinners()
     *
     *  FUNCTION DESCRIPTION
     *      This creates the spinners in the interface (filename Spinner and severity Spinner)
     *
     *  Side effects
     *      changes global variables severityspinner and filetypespinner
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     */
    public void createSpinners()
    {
        try {
            filetypespinner = (Spinner)findViewById(R.id.FilenameSpinner); //finds file spinner R.id
            ArrayAdapter<String> FileTypeadapter = new ArrayAdapter<String>(
                    this, android.R.layout.simple_spinner_item, FileSpinnerArray);
            FileTypeadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            filetypespinner.setAdapter(FileTypeadapter);
        }
        catch (Exception e)
        {
            messageBox("FilenameSpinner Error",
                    "File name Spinner Failed to create");
            e.printStackTrace();
            System.exit(ErrorCode);
        }

        try{
            severityspinner = (Spinner) findViewById(R.id.severitySpinner);
            ArrayAdapter<String> Severityadapter = new ArrayAdapter<String>(
                    this, android.R.layout.simple_spinner_item, SeveritySpinnerArray);
            Severityadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            severityspinner.setAdapter(Severityadapter);


        }
        catch(Exception e)
        {
            String currentLine = String.valueOf(getLineNumber());
            messageBox("initialization Error",
                    "Severity spinner Array failed to Initialize" + "Line:" + currentLine);
            e.printStackTrace();
            System.exit(ErrorCode);
        }

    }
    /**
     * End of createSpinners()
     * ----------------------------------------------------
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * launchApp(String packageName)
     *
     *  FUNCTION DESCRIPTION
     *      This is called to launch the map application through the use of an intent
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     * @param packageName
     */
    public void launchApp(String packageName) {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            startActivity(launchIntent);
        } else {
            Toast.makeText(MainActivity.this,
                    "There is no package available in android", Toast.LENGTH_LONG).show();
        }
    }
    /**
     * End of launchApp()
     * ----------------------------------------------------
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * findRdiobtn(int checkedID)
     *
     *  FUNCTION DESCRIPTION
     *      this links the R.id of the radio buttons to the relevant values.
     *      returns the value associated with the buttons set.
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     * @param checkedID
     */
    private int findRdiobtn(int checkedID) {
        switch (checkedID) {
            case R.id.rbtn100: return tinyData;
            case R.id.rbtn1000: return mediumData;
            case R.id.rbtn5000: return largeData;
            case R.id.rbtnAll: return readAll;
        }
        return ErrorCode;
    }
    /**
     * End of findRdiobtn()
     * ----------------------------------------------------
     */

    /**
     * -------------------------------------------------------------------------------------------------
     * getUserOptions(String fileSpinnerName)
     *
     *  FUNCTION DESCRIPTION
     *
     *
     * Side Effects
     *      Changes global variables: rdgMaxCountGroup, severityName, cSeverity
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     * @return
     */
    private int getUserOptions(String fileSpinnerName)
    {
        int rbID = 0;

        //Getting and checking that the route name format is reasonable
        int rnc = routeNameCheck();

        if(rnc != noError) {
            String currentLine = String.valueOf(getLineNumber());
            switch (rnc) {
                case originError:
                    messageBox("OriginError",
                            "no origin entered" + "Line:" + currentLine);
                    return(ErrorCode);

                case destinationError:
                    messageBox("DestinationError",
                            "no Destination entered" + "Line:" + currentLine);
                    return(ErrorCode);

                case locationFormatError:
                    messageBox("Format",
                            "Origin or Destination in wrong format" +
                                    "Line:" + currentLine);
                    return(ErrorCode);
            }
        }

        //This is about getting the file name.
        //if no file is selected AlertBox is shown
        if (fileSpinnerName == "Select a crime data file from the list") {
            //Alert Messages
            final AlertDialog filenameAlert = new AlertDialog.Builder(
                    MainActivity.this).create();
            filenameAlert.setTitle("Error");
            filenameAlert.setMessage("Please Select a filename option");
            filenameAlert.setButton(AlertDialog.BUTTON_NEUTRAL, "ok",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface filenameDialog, int i) {
                            filenameDialog.dismiss();
                        }
                    });
            filenameAlert.show();
            return(ErrorCode);
        }

        //Gets the number of lines to read from the input file
        //(Added for debugging but could have other purposes -- such as if data points were in
        // distance from region center then the number would control coverage)

        rdgMaxCountGroup = findViewById(R.id.MaxtoReadradioGroup);
        rbID = rdgMaxCountGroup.getCheckedRadioButtonId();
        RadioButton rb = (RadioButton) rdgMaxCountGroup.findViewById(rbID);
        String rbs = rb.getText().toString();
        if (rbs.equals("All")) {
            MaxtoRead = readAll;
        } else {
            MaxtoRead = Integer.parseInt(rbs);
        }

        rdgMaxCountGroup = findViewById(R.id.MaxtoReadradioGroup);
        int checkedID = rdgMaxCountGroup.getCheckedRadioButtonId();
        if (checkedID != -1)
        {
            //a Radiobtn is selected
            MaxtoRead = findRdiobtn(checkedID);
            if (MaxtoRead == ErrorCode)
            {
                String currentLine = String.valueOf(getLineNumber());
                messageBox("Data Input",
                        "Radio Button Error" + "Line:" + currentLine);
                System.exit(ErrorCode);
            }
        }
        else {
            String currentLine = String.valueOf(getLineNumber());
            //no Radiobtns are checked
            //should not happen as a rbtn is checked from the start
            messageBox("Data Input", "Radio Button Error" + "Line:" + currentLine);
            System.exit(ErrorCode);
        }


        //To do with the severities to include in the creation of crime incident circle
        //if no severity is selected AlertBox is shown

        severityName = severityspinner.getSelectedItem().toString();

        if (severityName == "Please Select a Severity") {
            //Alert Messages
            final AlertDialog filenameAlert = new AlertDialog.Builder(
                    MainActivity.this).create();
            filenameAlert.setTitle("Error");
            filenameAlert.setMessage("Please Select a severity option");
            filenameAlert.setButton(AlertDialog.BUTTON_NEUTRAL, "ok",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface filenameDialog, int i) {
                            filenameDialog.dismiss();
                            ;
                        }
                    });
            filenameAlert.show();
            return(ErrorCode);
        }

        //converts Severity string to double value
        cSeverity = SeverityStringToDouble(severityName);
        if(cSeverity == severityValueError)
        {
            String currentLine = String.valueOf(getLineNumber());
            messageBox("Data Error",
                    "Unable to decode severity string" + "Line:" + currentLine);
            System.exit(ErrorCode);
        }

        //sets the size of the incident circles according to the seekbar(userinput)
        sizeOfIncidentCircle = seekBarPosition;
        return (noError);
    }


    /**
     * ---------------------------------------------------------------------------------------------
     * processData()
     *
     *  FUNCTION DESCRIPTION
     *      Processes the data
     *      Reads user options
     *      Reads in the input file
     *      Writes the outputfile
     *      (See below)
     *
     *  SIDE EFFECTS
     *      changes global variables: Maxweight
     *      adds objects to: ActiveDataArray
     *
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     * @throws IOException
     */
    public void processData() throws IOException {
        int lineCount = 0;
        double Lng = 0.0;
        double Lat = 0.0;
        RadioButton rb;
        wDatapoint wdp;
        String readline;
        String[] wordtoken;
        String fileSpinnerName = filetypespinner.getSelectedItem().toString();

        if(getUserOptions(fileSpinnerName) == ErrorCode)
        {
            return;
        }

        //This checks the Read/Write permissions of the application.
        //Which depends on the build version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (!Settings.System.canWrite(this)) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                //This checks if the External storage location is writable.
                if (isExternalStorageReadWritable()) {
                    // StringBuilder sb = new StringBuilder();
                    try {
                        //Finds directory -- Using DOWNLOADS as is easily accessible for
                        //testing and debugging
                        //would change location on release version
                        File textFile = new File(
                                Environment.getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_DOWNLOADS),
                                fileSpinnerName); //usable due to legacy mode enabled

                        //Opens file and skips first line
                        FileInputStream fis = new FileInputStream(textFile);
                        if (fis != null) {
                            InputStreamReader isr = new InputStreamReader(fis);
                            BufferedReader buffread = new BufferedReader(isr);
                            buffread.readLine();// skips the headers

                            //for each line in file reads it and parses it
                            while ((readline = buffread.readLine()) != null)
                            {
                                wordtoken = readline.split(",");
                                if (wordtoken[9].length() > 0)
                                {
                                    //Check severity to see if line is wanted for further processing
                                    //if not discards and gets next line
                                    double SeverityValue = 0.0;
                                    SeverityValue = crimeTypeSeverity(wordtoken[9]);

                                    if (SeverityValue == severityValueError)
                                    {
                                        String currentLine = String.valueOf(getLineNumber());
                                        messageBox("Invalid Input",
                                                "Error in Severity Value read from file" +
                                                        "Line:" + currentLine);
                                        System.exit(ErrorCode);
                                    }

                                    if (SeverityValue <= cSeverity && SeverityValue !=
                                            severityValueError)
                                    {
                                        //Severity Value is required
                                        //So cont processing

                                        //now checks if the maximum line check has been exceeded
                                        //if false, continue processing
                                        lineCount++;
                                        if (lineCount <= MaxtoRead || MaxtoRead == readAll)
                                        {
                                            //Reads in and parses rest of the line
                                            if (wordtoken[4].length() > 0) {
                                                Lng = (Double.parseDouble(wordtoken[4]));
                                            }
                                            if (wordtoken[5].length() > 0) {
                                                Lat = (Double.parseDouble(wordtoken[5]));
                                            }
                                            //Active Data Array contains all required data to plot
                                            //circles -- contains:
                                            //circle centre
                                            //Circle weight
                                            //If empty adds first data point.
                                            if (ActiveDataArray.size() == 0) {
                                                // because it is a new entry it is 1 x SeverityValue
                                                wdp = new wDatapoint(Lat, Lng, SeverityValue);
                                                if (wdp.Wgt > MaxWeight) {
                                                    MaxWeight = wdp.Wgt;
                                                }
                                                ActiveDataArray.add(wdp);
                                            } else {
                                                //if active data contains circle works out if new
                                                //data point lies inside any existing circles
                                                //If not in a circle, Create another circle
                                                //If in a a circle increases the weight of the
                                                //existing circle
                                                int r = isnotClose(ActiveDataArray,
                                                        Lat, Lng, sizeOfIncidentCircle);
                                                if (r != circleBoundsError) {
                                                    if (r == isNotInCircle) {
                                                        // because it is a new entry
                                                        // it is 1 x SeverityValue
                                                        // rename severity value to Weight
                                                        wdp = new wDatapoint(Lat,Lng,SeverityValue);
                                                        if (wdp.Wgt > MaxWeight) {
                                                            MaxWeight = wdp.Wgt;
                                                        }
                                                        ActiveDataArray.add(wdp);
                                                    } else {
                                                        //in circle, no new circle created but
                                                        //weight of current circle increased
                                                        wdp = ActiveDataArray.get(r);
                                                        wdp.Wgt = wdp.Wgt + SeverityValue;
                                                        if (wdp.Wgt > MaxWeight) {
                                                            MaxWeight = wdp.Wgt;
                                                        }
                                                    }
                                                } else {
                                                    String currentLine;
                                                    currentLine = String.valueOf(getLineNumber());
                                                    messageBox("Data Error",
                                                            "There is an" +
                                                                    "underlying data error" +
                                                                    "Line:" + currentLine);
                                                    System.exit(ErrorCode);
                                                }
                                            }
                                        } else {
                                            String currentLine = String.valueOf(getLineNumber());
                                            messageBox("Data Error",
                                                    "There is an" +
                                                            "underlying data error" +
                                                            "Line:" + currentLine);
                                            System.exit(ErrorCode);
                                        }
                                    }
                                }
                            }
                            fis.close();
                        }
                        //readText = sb;
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        String currentLine = String.valueOf(getLineNumber());
                        messageBox("Data Processing Error",
                                "Error Processing Crime Data File" +
                                        "Line:" + currentLine);
                        System.exit(ErrorCode);
                    }
                }
            }
        }
        calculateHeatMapRange(MaxWeight);
        try{
            writeExternalFile();
        }
        catch(Exception e)
        {
            String currentLine = String.valueOf(getLineNumber());
            messageBox("Process Write Error",
                    "Error Writing to external File" + "Line:" + currentLine);
            e.printStackTrace();
            System.exit(ErrorCode);
        }
    }
    /**
     * End of processData()
     * ----------------------------------------------------
     *
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * calculateHeatMapRange(Double mW)
     *
     *  FUNCTION DESCRIPTION
     *  This works out the different colour bands depending on the size and the range
     *  of the data set weights.
     *  If the maximum weight is less than 7, allocates a number of bands depending on weight
     *  with one unit per band.
     *
     *  if the maximum weight is less than 7 then the bands are scaled in relation to the maxium
     *  weight.
     *
     *  returns nothing.
     *
     *  SideEffects:
     *      Changes the global variables: HeatMapColorNames[], HeatMapBreaks[]
     *
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     * @param mW
     */
    private void calculateHeatMapRange(double mW) {
        double x = 0.0;

        if(mW >= 6) {
            HeatMapColorNames = new String[HeatMapIncrements];
            HeatMapBreaks = new double[HeatMapIncrements];

            x = mW / HeatMapIncrements;

            //Assigns Colours to array positions
            HeatMapColorNames[0] = "Black";
            HeatMapColorNames[1] = "Blue";
            HeatMapColorNames[2] = "Green";
            HeatMapColorNames[3] = "Orange";
            HeatMapColorNames[4] = "Red";
            HeatMapColorNames[5] = "Yellow";
            HeatMapColorNames[6] = "White";

            //What does this do?
            try {
                for (int i = 0; i < HeatMapIncrements; i++) {
                    HeatMapBreaks[i] = Math.round((x*i) * 100) / 100; // rounds to 2 decimal places
                }
            } catch (Exception e) {
                String currentLine = String.valueOf(getLineNumber());
                messageBox("Heat map Error",
                        " Error Calculating HeatMap Range" + "Line:" + currentLine);
                e.printStackTrace();
                System.exit(ErrorCode);
            }
        }
        else
        {
            //This sets up the Heat map increments and the associated breakpoints (Buckets).
            //Done in a way to to allow for the dynamic compression of HeatMapIncrements data with
            //the number of buckets.
            //Set so that the highest value can be the full color array (HeatMapColorNames) which
            //has a size of 7 (arbitrary number choice)
            //Dynamic element done to prevent empty buckets created with sparse data. This algorithm
            //cannot run with empty buckets
            HeatMapIncrements = (int) Math.round(mW);
            HeatMapBreaks = new double[HeatMapIncrements + 1];
            HeatMapColorNames = new String[HeatMapIncrements + 1];

            switch(HeatMapIncrements)
            {
                case 1:
                    HeatMapColorNames[0] = "Black";
                    HeatMapBreaks[0] = 0;
                    HeatMapColorNames[1] = "White";
                    HeatMapBreaks[1] = 1;
                    break;
                case 2:
                    HeatMapColorNames[0] = "Black";
                    HeatMapBreaks[0] = 0;
                    HeatMapColorNames[1] = "Orange";
                    HeatMapBreaks[1] = 1;
                    HeatMapColorNames[2] = "White";
                    HeatMapBreaks[2] = 2;
                    break;
                case 3:
                    HeatMapColorNames[0] = "Black";
                    HeatMapBreaks[0] = 0;
                    HeatMapColorNames[1] = "Blue";
                    HeatMapBreaks[1] = 1;
                    HeatMapColorNames[2] = "Orange";
                    HeatMapBreaks[2] = 2;
                    HeatMapColorNames[3] = "White";
                    HeatMapBreaks[3] = 3;
                    break;
                case 4:
                    HeatMapColorNames[0] = "Black";
                    HeatMapBreaks[0] = 0;
                    HeatMapColorNames[1] = "Blue";
                    HeatMapBreaks[1] = 1;
                    HeatMapColorNames[2] = "Orange";
                    HeatMapBreaks[2] = 2;
                    HeatMapColorNames[3] = "Yellow";
                    HeatMapBreaks[3] = 3;
                    HeatMapColorNames[4] = "White";
                    HeatMapBreaks[4] = 4;
                    break;
                case 5:
                    HeatMapColorNames[0] = "Black";
                    HeatMapBreaks[0] = 0;
                    HeatMapColorNames[1] = "Blue";
                    HeatMapBreaks[1] = 1;
                    HeatMapColorNames[2] = "Green";
                    HeatMapBreaks[2] = 2;
                    HeatMapColorNames[3] = "Orange";
                    HeatMapBreaks[3] = 3;
                    HeatMapColorNames[4] = "Yellow";
                    HeatMapBreaks[4] = 4;
                    HeatMapColorNames[5] = "White";
                    HeatMapBreaks[5] = 5;
                    break;
            }
            HeatMapIncrements+=1; //keeping the parent array size and array contents the uniform.
        }
    }

    /**
     * * End of calculateHeatMapRange()
     * *---------------------------------------------------
     *
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * isnotClose(ArrayList<wDatapoint> ActiveDataArray, double Lat, double Lng, double Distance)
     *
     *  FUNCTION DESCRIPTION
     *  Works out if a data point is within an existing circle
     *  if not then a new circle is added.
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     * @param ActiveDataArray
     * @param Lat
     * @param Lng
     * @param sizeOfIncidentCircle
     * @return
     */
    private int isnotClose(ArrayList<wDatapoint> ActiveDataArray,
                           double Lat, double Lng, double sizeOfIncidentCircle) {

        for (int circleNumber = 0; circleNumber < ActiveDataArray.size(); circleNumber++)
        {
            double xLat = ActiveDataArray.get(circleNumber).Lat;
            double xLng = ActiveDataArray.get(circleNumber).Lng;

            // Gets distanceBetween()
            double d = distanceBetween(Lat, Lng, xLat, xLng, distanceUnits);
            if (d >= sizeOfIncidentCircle)
            {
                return isNotInCircle;
            } else {
                return circleNumber;
            }
        }
        return circleBoundsError;
    }
    /**
     * * End of isnotClose
     * *---------------------------------------------------
     *
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * isExternalStorageReadWritable()
     *
     *  FUNCTION DESCRIPTION
     *  Called to where Read/Write permissions are needed to be checked.
     *  Checks if media permissions are mounted and read/Writable.
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     * @return
     */
    private boolean isExternalStorageReadWritable() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(
                Environment.getExternalStorageState())) {
            return true;
        } else {
            return false;
        }
    }
    /**
     * End of isExternalStorageReadWritable()
     * ----------------------------------------------------
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * writeExternalFile()
     *
     *  FUNCTION DESCRIPTION
     *      This checks the devices permissions, enables them if needed.
     *      Checks given Directory(External Storage) for a valid location and writes file.
     *      File is written in an Adapted JSON format(SeeBelow)
     *
     *  Side Effects
     *      Changes Global variable: bufferString
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     */
    public void writeExternalFile() {
        //Request permissions for read and write at run time
        //Also checks if permissions have been obtained
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (!Settings.System.canWrite(this))
            {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

                if (isExternalStorageReadWritable())
                {
                    int adaNumber = ActiveDataArray.size(); // initializes the activeDataArray
                    File writeTextfile = new File(
                            Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOWNLOADS),
                            cdDataFile); //enabled legacy storage allows use
                    FileOutputStream outputStream;
                    try {
                        //Opens and Writes the crime data file to communicate with the other app
                        writeTextfile.createNewFile();
                        outputStream = new FileOutputStream(writeTextfile, false);

                        //Crime data file has this format
                        //Line 1 Contains: Control Data to pass to the other app
                        //Rest of lines Contains: Data to pass to the other app
                        //Line 1 is formatted with Origin; Destination; Size of IncidentCircle;
                        // Units of Distance; HeatMapStructure
                        //The HeadMapStructure is defined as a sequence of HeatMapColorName
                        // as a String and HeatMapBreak as a double
                        //The rest of the lines are formatted as:
                        //{"lat" : Latitude , "lng" : Longitude , "wgt" : Weight},
                        //Latitude is the latitude of the crime,
                        // Longitude is the Longitude of the crime
                        //End of file is marked with a "]"
                        bufferString = routeOrigin + ";"
                                + routeDestination + ";"
                                + sizeOfIncidentCircle + ";"
                                + distanceUnits + ";"
                                + HeatMapIncrements + ";";

                        for (int j = 0; j < HeatMapIncrements; j++)
                        {
                            bufferString = bufferString + HeatMapColorNames[j] + ":"
                                    + HeatMapBreaks[j] + ":";
                        }

                        bufferString = bufferString + "\n";
                        outputStream.write(bufferString.getBytes());

                        for (int i = 0; i < adaNumber - 1; i++) {
                            wDatapoint wdp = ActiveDataArray.get(i);

                            bufferString = "{\"lat\" : " + wdp.Lat +
                                    ", \"lng\" : " + wdp.Lng +
                                    ", \"wgt\" : " + wdp.Wgt +
                                    " }";

                            if (i != adaNumber - 1) {
                                bufferString = bufferString + ",\n";
                            } else {
                                bufferString = bufferString + "\n"; // if last one
                            }

                            outputStream.write(bufferString.getBytes());
                        }
                        //Adds end of file Character
                        bufferString = "]\n";

                        //Final write -- cleans stream and closes it
                        outputStream.write(bufferString.getBytes());
                        outputStream.flush();
                        outputStream.close();
                    }
                    catch (Exception e)
                    {
                        String currentLine = String.valueOf(getLineNumber());
                        e.printStackTrace();
                        messageBox("Write file Error",
                                e.getMessage() + "Line:" + currentLine);
                    }
                }
            }
        }
    }
    /**
     * End of writeExternalFile()
     * ----------------------------------------------------
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * SeverityStringToDouble(String ct)
     *
     *  FUNCTION DESCRIPTION
     *  This Converts the Severity Strings into double values
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     * @param ct
     * @return
     */
    public double SeverityStringToDouble(String ct) {
        if (Severity1.equals(ct)) { return severityValue1; }
        if (Severity2.equals(ct)) { return severityValue2; }
        if (Severity3.equals(ct)) { return severityValue3; }
        if (Severity4.equals(ct)) { return severityValue4; }
        if (SeverityAll.equals(ct)) { return severityValueAll; }
        return severityValueError; // Unknown value
    }
    /**
     * * End of SeverityStringToDouble()
     * *---------------------------------------------------
     *
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * crimeTypeSeverity(String ct)
     *
     *  FUNCTION DESCRIPTION
     *  This equates the raw file crime types to different severities and groups them together.
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     * @param ct
     * @return
     */
    public double crimeTypeSeverity(String ct) {
        String ctlc = ct.toLowerCase(); //Adds uniformity to prevent errors
        if ("violence and sexual offences".equals(ctlc)) { return severityValue1; }

        if ("vehicle crime".equals(ctlc) ||
                "bicycle theft".equals(ctlc) ||
                "burglary".equals(ctlc) ||
                "other theft".equals(ctlc) ||
                "robbery".equals(ctlc) ||
                "shoplifting".equals(ctlc) ||
                "theft from the person".equals(ctlc) ||
                "possession of weapons".equals(ctlc)) {
            return severityValue2; }

        if ("drugs".equals(ctlc) ||
                "criminal damage and arson".equals(ctlc) ||
                "anti-social behaviour".equals(ctlc) ||
                "public order".equals(ctlc)) { return severityValue3; }

        if (("other crime").equals(ctlc)) { return severityValue4; }

        return severityValueError; // Unknown value
    }
    /**
     * * End of crimeTypeSeverity()
     * *---------------------------------------------------
     *
     */


    /**
     * -------------------------------------------------------------------------------------------------
     * getLineNumber()
     *
     *  FUNCTION DESCRIPTION
     *      accompanying function to ___8drrd3148796d_Xaf()
     *
     * @author Brian_Entei
     * @version 1.0
     * @since 1/08/2021
     * Accessed: 30/07/2021
     * Reference:
     * https://stackoverflow.com/questions/17473148/dynamically-get-the-current-line-number%EF%BC%89
     * ---------------------------------------------------
     * @return The line number of the code that ran this method
     * */
    public static int getLineNumber() {
        return ___8drrd3148796d_Xaf();
    }
    /**
     * * End of getLineNumber
     * *---------------------------------------------------
     *
     */


    /**
     * -------------------------------------------------------------------------------------------------
     * ___8drrd3148796d_Xaf()
     *
     *  FUNCTION DESCRIPTION
     *      This methods name is ridiculous on purpose to prevent any other method
     *      names in the stack trace from potentially matching this one.
     *      (See reference for more detail)
     *
     * @author Brian_Entei
     * @version 1.0
     * @since 1/08/2021
     * Accessed: 30/07/2021
     * Reference:
     * https://stackoverflow.com/questions/17473148/dynamically-get-the-current-line-number%EF%BC%89
     * ---------------------------------------------------
     * @return
     */
    private static int ___8drrd3148796d_Xaf() {
        boolean thisOne = false;
        int thisOneCountDown = 1;
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        for(StackTraceElement element : elements) {
            String methodName = element.getMethodName();
            int lineNum = element.getLineNumber();
            if(thisOne && (thisOneCountDown == 0)) {
                return lineNum;
            } else if(thisOne) {
                thisOneCountDown--;
            }
            if(methodName.equals("___8drrd3148796d_Xaf")) {
                thisOne = true;
            }
        }
        return -1;
    }
}
/**
 * * End of ___8drrd3148796d_Xaf()
 * *---------------------------------------------------
 *
 */

/**
 * ---------------------------------------------------------------------------------------------
 * END OF CLASS
 * ---------------------------------------------------------------------------------------------
 */
