import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;


public class WeatherApp {
    //fetch weather data for given location
    public static JSONObject getWeatherData(String locationName) {
        //get location coordinates using the geolocation API
        JSONArray locationData = getLocationData(locationName);

        if (locationData == null || locationData.isEmpty()) {
            System.out.println("Error: Could not get location data.");
            return null;
        }

        //extract latitude and longitude data
        JSONObject location = (JSONObject) locationData.get(0);
        double latitude = (double) location.get("latitude");
        double longitude = (double) location.get("longitude");


        //build API request URL with location coordinates
        String urlString = "https://api.open-meteo.com/v1/forecast?" +
                "latitude=" + latitude + "&longitude=" + longitude +
                "&hourly=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&timezone=America%2FNew_York";

        try {
            //call API and get response
            HttpURLConnection conn = fetchApiResponse(urlString);

            //check for response status code
            //200 = success
            if (conn.getResponseCode() != 200) {
                System.out.println("Error: Could not connect to the API.");
                return null;
            }

            //store resulting JSON data
            StringBuilder resultJson = new StringBuilder();
            Scanner scanner = new Scanner(conn.getInputStream());
            while (scanner.hasNext()) {
                //read and store into the string builder
                resultJson.append(scanner.nextLine());
            }

            //close scanner
            scanner.close();

            //close url connection
            conn.disconnect();

            //parse through the data
            JSONParser parser = new JSONParser();
            JSONObject resultJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));

            //retrieve hourly data
            JSONObject hourly = (JSONObject) resultJsonObj.get("hourly");

            //objective is to get the current hour's data
            //and for that the index of the current hour is needed
            JSONArray time = (JSONArray) hourly.get("time");
            int index = findIndexOfCurrentTime(time);

            //get temperature
            JSONArray temperatureData = (JSONArray) hourly.get("temperature_2m");
            double temperature = (double) temperatureData.get(index);

            //get weather code
            JSONArray weatherCode = (JSONArray) hourly.get("weather_code");
            String weatherCondition = convertWeatherCode((long) weatherCode.get(index));

            //get humidity
            JSONArray relativeHumidity = (JSONArray) hourly.get("relative_humidity_2m");
            long humidity = (long) relativeHumidity.get(index);

            //get wind speed
            JSONArray windspeedData = (JSONArray) hourly.get("wind_speed_10m");
            double windspeed = (double) windspeedData.get(index);

            //build the weather JSON data object that the frontend will have access
            JSONObject weatherData = new JSONObject();
            weatherData.put("temperature", temperature);
            weatherData.put("weather_condition", weatherCondition);
            weatherData.put("humidity", humidity);
            weatherData.put("windspeed", windspeed);

            return weatherData;

        }catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    //retrieves geographic coordinates for given location name
    public static JSONArray getLocationData(String locationName) {
        //replace any whitespace in location name to + to adhere to API's request format
        locationName = locationName.replaceAll(" ", "+");

        //build API url with location parameter
        String urlString = "https://geocoding-api.open-meteo.com/v1/search?name=" +
                locationName + "&count=10&language=en&format=json";

        try {
            //call api and get response
            HttpURLConnection conn = fetchApiResponse(urlString);

            //check response status
            //200 = successful connection
            if (conn.getResponseCode() != 200) {
                System.out.println("Error: Could not connect to API");
                return null;
            }else {
                //store the API results
                StringBuilder resultJson = new StringBuilder();
                Scanner scanner = new Scanner(conn.getInputStream());

                //read and store the resulting json data into our string builder
                while (scanner.hasNext()) {
                    resultJson.append(scanner.nextLine());
                }

                //close scanner
                scanner.close();

                //close url connection
                conn.disconnect();

                //parse the JSON string into a JSON obj
                JSONParser parser = new JSONParser();
                JSONObject resultJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));

                //get the list of location data the API generated from the location name
                JSONArray locationData = (JSONArray) resultJsonObj.get("results");
                return locationData;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }

        //could not find location
        return null;
    }

    private static HttpURLConnection fetchApiResponse(String urlString) {
        try {
            //attempt to create connection
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            //set request method to get
            conn.setRequestMethod("GET");

            //connect to the API
            conn.connect();
            return conn;
        }catch (IOException e) {
            e.printStackTrace();
        }

        //could not make connection
        return null;
    }

    private static int findIndexOfCurrentTime(JSONArray timeList) {
        String currentTime = getCurrentTime();

        //iterate through the time list and see which one matches our current time
        for (int i = 0; i < timeList.size(); i++) {
            String time = (String) timeList.get(i);
            if (time.equalsIgnoreCase(currentTime)) {
                //return the index
                return i;
            }
        }
        System.out.println("No match found. Using index 0.");
        return 0;
    }

    public static String getCurrentTime() {
        //get current data and time
        LocalDateTime currentDateTime = LocalDateTime.now();

        //format date to be 2024-02-02T00:00 (This is the API's format)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00");

        //format and print the current date and time
        String formattedDateTime = currentDateTime.format(formatter);

        return formattedDateTime;
    }

    private static String convertWeatherCode(long weathercode) {
        String weatherCondition = "";
        if (weathercode ==0L) {
            //clear
            weatherCondition = "Clear";
        } else if (weathercode <= 3L && weathercode > 0L) {
            //Cloudy
            weatherCondition = "Cloudy";
        } else if ((weathercode >= 51L && weathercode <= 67L)
                || (weathercode >= 80L && weathercode <= 99L)) {
            //Rain
            weatherCondition = "Rain";
        } else if (weathercode >= 71L && weathercode <=77L) {
            //Snow
            weatherCondition = "Snow";
        }

        return weatherCondition;
    }
}
