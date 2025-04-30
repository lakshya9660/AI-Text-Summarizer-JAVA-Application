package com.textsummarizer;

import java.io.File;
import java.io.IOException;

import com.textsummarizer.service.HuggingFaceService;
import com.textsummarizer.util.PdfProcessor;
import com.textsummarizer.util.WebScraper;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Main extends Application {

    private TextArea inputTextArea;
    private TextArea outputTextArea;
    private ComboBox<String> summaryStyleComboBox;
    private ComboBox<String> perspectiveComboBox;
    private Button summarizeButton;
    private Button uploadPdfButton;
    private TextField urlTextField;
    private Button fetchUrlButton;
    private HuggingFaceService huggingFaceService;
    private ProgressIndicator progressIndicator;

    @Override
    public void start(Stage primaryStage) {
        // Initialize HuggingFace service
        huggingFaceService = new HuggingFaceService();
        
        // Create the main layout
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.5);

        // Left side - Input
        VBox inputBox = createInputBox();
        splitPane.getItems().add(inputBox);

        // Right side - Output
        VBox outputBox = createOutputBox();
        splitPane.getItems().add(outputBox);

        // Create the scene with dark background
        Scene scene = new Scene(splitPane, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        // Add glow effect to the window
        DropShadow windowGlow = new DropShadow();
        windowGlow.setColor(Color.valueOf("#00ff9d"));
        windowGlow.setRadius(20);
        splitPane.setEffect(windowGlow);

        // Set up the stage
        primaryStage.initStyle(StageStyle.UNIFIED);
        primaryStage.setTitle("AI Text Summarizer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createInputBox() {
        VBox inputBox = new VBox(16);
        inputBox.setPadding(new Insets(20));
        inputBox.setAlignment(Pos.TOP_CENTER);

        // Title
        Label titleLabel = new Label("AI Text Summarizer");
        titleLabel.setFont(Font.font("Roboto", FontWeight.BOLD, 24));
        titleLabel.setStyle("-fx-text-fill: #00ff9d;");

        // Input text area with title
        Label inputLabel = new Label("Input Text");
        inputLabel.setFont(Font.font("Roboto", FontWeight.BOLD, 16));
        inputTextArea = new TextArea();
        inputTextArea.setPrefRowCount(10);
        inputTextArea.setWrapText(true);
        inputTextArea.setPromptText("Enter or paste your text here...");

        // URL input with icon
        HBox urlBox = new HBox(8);
        urlBox.setAlignment(Pos.CENTER_LEFT);
        urlTextField = new TextField();
        urlTextField.setPromptText("Enter URL to fetch content...");
        urlTextField.setPrefWidth(300);
        fetchUrlButton = new Button("Fetch URL");
        fetchUrlButton.setOnAction(e -> handleUrlFetch());
        urlBox.getChildren().addAll(urlTextField, fetchUrlButton);

        // PDF upload button with icon
        uploadPdfButton = new Button("Upload PDF");
        uploadPdfButton.setOnAction(e -> handlePdfUpload());

        // Controls section
        VBox controlsBox = new VBox(12);
        controlsBox.setStyle("-fx-padding: 15px; -fx-border-color: #00ff9d22; -fx-border-radius: 5px;");

        // Summary style selection
        Label styleLabel = new Label("Summary Style");
        styleLabel.setFont(Font.font("Roboto", FontWeight.BOLD, 14));
        summaryStyleComboBox = new ComboBox<>();
        summaryStyleComboBox.getItems().addAll(
            "Standard Summary",
            "Bullet Point Summary",
            "Executive Summary",
            "Technical Summary"
        );
        summaryStyleComboBox.setValue("Standard Summary");
        summaryStyleComboBox.setMaxWidth(Double.MAX_VALUE);

        // Perspective selection
        Label perspectiveLabel = new Label("Perspective");
        perspectiveLabel.setFont(Font.font("Roboto", FontWeight.BOLD, 14));
        perspectiveComboBox = new ComboBox<>();
        perspectiveComboBox.getItems().addAll(
            "Executive",
            "Student",
            "Developer",
            "Debate Mode"
        );
        perspectiveComboBox.setValue("Executive");
        perspectiveComboBox.setMaxWidth(Double.MAX_VALUE);

        controlsBox.getChildren().addAll(
            styleLabel, summaryStyleComboBox,
            perspectiveLabel, perspectiveComboBox
        );

        // Progress indicator and summarize button in HBox
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER);
        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(30, 30);
        
        summarizeButton = new Button("Summarize");
        summarizeButton.setStyle("-fx-font-size: 14px; -fx-padding: 10 20;");
        summarizeButton.setOnAction(e -> handleSummarize());
        
        actionBox.getChildren().addAll(progressIndicator, summarizeButton);

        // Add all components to input box
        inputBox.getChildren().addAll(
            titleLabel,
            inputLabel,
            inputTextArea,
            urlBox,
            uploadPdfButton,
            controlsBox,
            actionBox
        );

        return inputBox;
    }

    private VBox createOutputBox() {
        VBox outputBox = new VBox(16);
        outputBox.setPadding(new Insets(20));
        outputBox.setAlignment(Pos.TOP_CENTER);

        Label outputLabel = new Label("Summary Output");
        outputLabel.setFont(Font.font("Roboto", FontWeight.BOLD, 16));
        
        outputTextArea = new TextArea();
        outputTextArea.setEditable(false);
        outputTextArea.setWrapText(true);
        outputTextArea.setPrefRowCount(20);
        outputTextArea.setPromptText("Generated summary will appear here...");

        outputBox.getChildren().addAll(outputLabel, outputTextArea);
        return outputBox;
    }

    private void handlePdfUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                String text = PdfProcessor.extractTextFromPdf(file);
                inputTextArea.setText(text);
            } catch (IOException e) {
                showError("PDF Processing Error", "Failed to process PDF file: " + e.getMessage());
            }
        }
    }

    private void handleUrlFetch() {
        String url = urlTextField.getText().trim();
        if (url.isEmpty()) {
            showError("URL Error", "Please enter a valid URL");
            return;
        }

        try {
            String text = WebScraper.extractTextFromUrl(url);
            inputTextArea.setText(text);
        } catch (IOException e) {
            showError("URL Fetch Error", "Failed to fetch content from URL: " + e.getMessage());
        }
    }

    private void handleSummarize() {
        String inputText = inputTextArea.getText().trim();
        if (inputText.isEmpty()) {
            showError("Input Error", "Please enter some text to summarize");
            return;
        }

        String style = summaryStyleComboBox.getValue();
        String perspective = perspectiveComboBox.getValue();

        // Show progress indicator
        progressIndicator.setVisible(true);
        summarizeButton.setDisable(true);

        // Run summarization in a background thread
        new Thread(() -> {
            try {
                String summary = huggingFaceService.summarizeText(inputText, style, perspective);
                Platform.runLater(() -> {
                    outputTextArea.setText(summary);
                    progressIndicator.setVisible(false);
                    summarizeButton.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Summarization Error", "Failed to generate summary: " + e.getMessage());
                    progressIndicator.setVisible(false);
                    summarizeButton.setDisable(false);
                });
            }
        }).start();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
} 