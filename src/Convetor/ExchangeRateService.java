package Convetor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

/**
 * Service class to fetch exchange rates from the ExchangeRate-API.
 * * This class uses a very basic and fragile method to parse JSON.
 * For a production application, please use a dedicated JSON library like
 * Gson, Jackson, or org.json.
 */
public class ExchangeRateService {

    // !!! IMPORTANT !!!
    // 1. Sign up for a free API key at https://www.exchangerate-api.com
    // 2. Paste your API key here.
    private static final String API_KEY = "bdcda7cce9ec9adb7613991c";

    private static final String API_BASE_URL = "https://v6.exchangerate-api.com/v6/";

    /**
     * Fetches the exchange rate between two currencies.
     *
     * @param fromCurrency The 3-letter currency code to convert from (e.g., "USD").
     * @param toCurrency   The 3-letter currency code to convert to (e.g., "EUR").
     * @return The conversion rate (e.g., 1 USD = 0.92 EUR, returns 0.92).
     * @throws Exception If the API key is missing, or if there's a network or parsing error.
     */
    public double getExchangeRate(String fromCurrency, String toCurrency) throws Exception {

        if (API_KEY == null || API_KEY.equals("YOUR_API_KEY_HERE")) {
            throw new Exception("API Key is not set in ExchangeRateService.java. " +
                    "Please get a free key from https://www.exchangerate-api.com");
        }

        // 1. Construct the API URL
        // We fetch the rates based on the 'fromCurrency'
        String urlString = API_BASE_URL + API_KEY + "/latest/" + fromCurrency;

        // 2. Make the HTTP Request
        URL url = new URI(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("API Request Failed. Response Code: " + responseCode);
        }

        // 3. Read the JSON Response
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        // 4. Parse the JSON Response (Basic, Fragile Parsing)
        // A typical response snippet looks like:
        // "... "conversion_rates":{"USD":1,"AED":3.67,"EUR":0.92, ...} ..."
        String jsonResponse = response.toString();

        // Find the conversion_rates object
        String ratesMarker = "\"conversion_rates\":{";
        int ratesStartIndex = jsonResponse.indexOf(ratesMarker);
        if (ratesStartIndex == -1) {
            // Check for error response
            if (jsonResponse.contains("\"result\":\"error\"")) {
                throw new Exception("API returned an error: " + jsonResponse);
            }
            throw new Exception("Could not find 'conversion_rates' in API response.");
        }

        // Find the target currency within the conversion_rates
        // We search for a pattern like "EUR":0.92
        String toCurrencyMarker = "\"" + toCurrency + "\":";
        int currencyStartIndex = jsonResponse.indexOf(toCurrencyMarker, ratesStartIndex);

        if (currencyStartIndex == -1) {
            throw new Exception("Currency code '" + toCurrency + "' not found in API response.");
        }

        // Find the start of the rate value
        int rateValueStartIndex = currencyStartIndex + toCurrencyMarker.length();

        // Find the end of the rate value (it ends at a comma ',' or a closing brace '}')
        int rateValueEndIndex = -1;
        int commaIndex = jsonResponse.indexOf(",", rateValueStartIndex);
        int braceIndex = jsonResponse.indexOf("}", rateValueStartIndex);

        if (commaIndex != -1 && braceIndex != -1) {
            rateValueEndIndex = Math.min(commaIndex, braceIndex);
        } else if (commaIndex != -1) {
            rateValueEndIndex = commaIndex;
        } else if (braceIndex != -1) {
            rateValueEndIndex = braceIndex;
        } else {
            throw new Exception("Could not parse rate value for " + toCurrency);
        }

        // Extract the rate value as a string
        String rateString = jsonResponse.substring(rateValueStartIndex, rateValueEndIndex).trim();

        // 5. Return the parsed rate
        return Double.parseDouble(rateString);
    }
}
