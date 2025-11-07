package Convetor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

/**
 * Main application class for the Currency Converter.
 * This class creates the Swing-based GUI and handles user interactions.
 */
public class CurrencyConverter extends JFrame {

    // GUI Components
    private JComboBox<String> fromCurrencyBox;
    private JComboBox<String> toCurrencyBox;
    private JTextField amountField;
    private JLabel resultLabel;
    private JButton convertButton;
    private JLabel loadingLabel;

    // Service for fetching exchange rates
    private ExchangeRateService exchangeRateService;

    public CurrencyConverter() {
        this.exchangeRateService = new ExchangeRateService();

        // --- Setup Frame ---
        setTitle("Currency Converter");
        setSize(450, 350); // Increased size for better layout
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center on screen

        // --- Main Panel ---
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // Padding
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8); // Spacing between components

        // --- Title ---
        JLabel titleLabel = new JLabel("Currency Converter");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        mainPanel.add(titleLabel, gbc);

        // --- Amount ---
        gbc.gridwidth = 1; // Reset grid width
        gbc.gridy = 1;
        gbc.gridx = 0;
        mainPanel.add(new JLabel("Amount:"), gbc);

        gbc.gridx = 1;
        amountField = new JTextField("1.00");
        amountField.setFont(new Font("Arial", Font.PLAIN, 16));
        mainPanel.add(amountField, gbc);

        // --- From Currency ---
        gbc.gridy = 2;
        gbc.gridx = 0;
        mainPanel.add(new JLabel("From:"), gbc);

        gbc.gridx = 1;
        fromCurrencyBox = new JComboBox<>(getAvailableCurrencies());
        fromCurrencyBox.setSelectedItem("USD"); // Default
        mainPanel.add(fromCurrencyBox, gbc);

        // --- To Currency ---
        gbc.gridy = 3;
        gbc.gridx = 0;
        mainPanel.add(new JLabel("To:"), gbc);

        gbc.gridx = 1;
        toCurrencyBox = new JComboBox<>(getAvailableCurrencies());
        toCurrencyBox.setSelectedItem("EUR"); // Default
        mainPanel.add(toCurrencyBox, gbc);

        // --- Convert Button ---
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        convertButton = new JButton("Convert");
        convertButton.setFont(new Font("Arial", Font.BOLD, 16));
        convertButton.setBackground(new Color(66, 134, 244));
        convertButton.setForeground(Color.WHITE);
        convertButton.setOpaque(true);
        convertButton.setBorderPainted(false);
        mainPanel.add(convertButton, gbc);

        // --- Result Label ---
        gbc.gridy = 5;
        resultLabel = new JLabel(" ");
        resultLabel.setFont(new Font("Arial", Font.BOLD, 18));
        resultLabel.setHorizontalAlignment(JLabel.CENTER);
        mainPanel.add(resultLabel, gbc);

        // --- Loading Label (hidden by default) ---
        gbc.gridy = 6;
        loadingLabel = new JLabel("Converting...");
        loadingLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        loadingLabel.setHorizontalAlignment(JLabel.CENTER);
        loadingLabel.setVisible(false);
        mainPanel.add(loadingLabel, gbc);

        // --- Action Listener for the Button ---
        convertButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performConversion();
            }
        });

        // Add main panel to frame
        add(mainPanel);
    }

    /**
     * Fetches available currency codes from Java's Locale data.
     * @return A sorted array of currency codes (e.g., "USD", "EUR").
     */
    private String[] getAvailableCurrencies() {
        Set<String> currencyCodes = new TreeSet<>();
        for (Locale locale : Locale.getAvailableLocales()) {
            try {
                Currency currency = Currency.getInstance(locale);
                currencyCodes.add(currency.getCurrencyCode());
            } catch (Exception e) {
                // Ignore locales without valid currencies
            }
        }
        return currencyCodes.toArray(new String[0]);
    }

    /**
     * Handles the conversion logic when the button is clicked.
     * Uses a SwingWorker to perform the network request off the main GUI thread.
     */
    private void performConversion() {
        // 1. Get user input
        String fromCurrency = (String) fromCurrencyBox.getSelectedItem();
        String toCurrency = (String) toCurrencyBox.getSelectedItem();
        String amountText = amountField.getText();

        // 2. Validate input
        double amount;
        try {
            amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                resultLabel.setText("Amount must be positive.");
                resultLabel.setForeground(Color.RED);
                return;
            }
        } catch (NumberFormatException e) {
            resultLabel.setText("Invalid amount.");
            resultLabel.setForeground(Color.RED);
            return;
        }

        // 3. Perform conversion in background thread
        setLoading(true);

        // SwingWorker is used to perform network I/O in a background thread
        // and update the GUI on the Event Dispatch Thread (EDT).
        SwingWorker<Double, Void> worker = new SwingWorker<Double, Void>() {
            @Override
            protected Double doInBackground() throws Exception {
                // This runs on a background thread
                return exchangeRateService.getExchangeRate(fromCurrency, toCurrency);
            }

            @Override
            protected void done() {
                // This runs on the EDT after doInBackground() finishes
                try {
                    // Get the result from the background task
                    double rate = get();

                    // Calculate the final result
                    BigDecimal result = new BigDecimal(amount * rate);
                    result = result.setScale(2, RoundingMode.HALF_UP); // Round to 2 decimal places

                    // Update the GUI
                    resultLabel.setText(
                            String.format("%.2f %s = %.2f %s", amount, fromCurrency, result.doubleValue(), toCurrency)
                    );
                    resultLabel.setForeground(Color.BLACK);

                } catch (InterruptedException | ExecutionException e) {
                    // Handle errors from the background task
                    e.printStackTrace(); // Log the full error
                    resultLabel.setText("Error fetching rate. Check console.");
                    resultLabel.setForeground(Color.RED);
                } finally {
                    // Hide loading indicator
                    setLoading(false);
                }
            }
        };

        worker.execute(); // Start the background task
    }

    /**
     * Toggles the UI state between loading and idle.
     * @param isLoading true to show loading state, false to show idle state
     */
    private void setLoading(boolean isLoading) {
        convertButton.setEnabled(!isLoading);
        loadingLabel.setVisible(isLoading);
        if (isLoading) {
            resultLabel.setText(" "); // Clear previous result
        }
    }

    /**
     * Main method to run the application.
     */
    public static void main(String[] args) {
        // Ensure GUI updates are done on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            new CurrencyConverter().setVisible(true);
        });
    }
}
