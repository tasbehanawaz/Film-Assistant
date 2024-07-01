package ac.kent;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.imageio.ImageIO;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.concurrent.ExecutionException;
import java.util.List;


import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class Main {
    private JFrame frame;

    private JLabel creativityLabel;
    private JComboBox creativityLevel;


    private JLabel filterLabel;
    private JComboBox filterType;

    private JTextField inputField;
    private JTextArea storyArea;
    private JLabel imageLabel;
    private JTextArea imageCaption;

    private JButton generateButton;
    private JMenuBar menuBar;

    private String baseDataPath;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Main() {
        frame = new JFrame("Film Assistant");

        creativityLabel = new JLabel("Creativity Level:");
        creativityLevel = new JComboBox(new String[]{"Low", "Medium", "High"});

        filterLabel = new JLabel("Filter Type:");
        filterType = new JComboBox(new String[]{"None", "Grayscale", "Sepia"});

        inputField = new JTextField();

        storyArea = new JTextArea();
        storyArea.setLineWrap(true);
        storyArea.setWrapStyleWord(true);

        imageLabel = new JLabel();
        imageCaption = new JTextArea();
        imageCaption.setLineWrap(true);
        imageCaption.setWrapStyleWord(true);

        generateButton = new JButton("Generate Story");
        menuBar = new JMenuBar();

        // Setup the menu bar
        setupMenuBar();

        // Initialize layout
        initializeLayout();

        // Add button listener for generating the story
        setupButtonListener();

        // Set default close operation and pack
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        baseDataPath = System.getProperty("user.dir") + "/data/";

        // frame.setSize(1200, 750);
    }

    private void setupMenuBar() {
        JMenu fileMenu = new JMenu("File");
        JMenu helpMenu = new JMenu("Help");
        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        frame.setJMenuBar(menuBar);
    }

    private void initializeLayout() {
        // Main panel with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10)); // Add horizontal and vertical gaps

        // Top panel for input fields with FlowLayout for horizontal alignment
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5)); // Add horizontal and vertical gaps

        inputField.setPreferredSize(new Dimension(600, 35)); // Increase the preferred width for input field

        creativityLabel.setPreferredSize(new Dimension(100, 35)); // Set preferred size for label
        creativityLevel.setPreferredSize(new Dimension(100, 35)); // Set preferred size for combo box

        filterLabel.setPreferredSize(new Dimension(100, 35)); // Set preferred size for label
        filterType.setPreferredSize(new Dimension(100, 35)); // Set preferred size for combo box

        topPanel.add(inputField);
        topPanel.add(creativityLabel);
        topPanel.add(creativityLevel);
        topPanel.add(filterLabel);
        topPanel.add(filterType);


        // Center panel for story area
        JScrollPane storyScrollPane = new JScrollPane(storyArea);
        storyScrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add padding around scroll pane
        JScrollPane captionScrollPane = new JScrollPane(imageCaption); // Declare and initialize captionScrollPane
        captionScrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Add padding around caption scroll pane
        storyArea.setPreferredSize(new Dimension(700, 700)); // Set preferred size using a Dimension object

        // East panel for the image and its caption
        JPanel eastPanel = new JPanel(new BorderLayout(5, 5)); // Add horizontal and vertical gaps
        imageLabel.setPreferredSize(new Dimension(600, 400)); // Set preferred size for image label
        eastPanel.add(imageLabel, BorderLayout.CENTER);
        eastPanel.add(captionScrollPane, BorderLayout.SOUTH); // Use the initialized captionScrollPane

        // Bottom panel for the generate button with padding
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); // Add padding around the bottom panel
        bottomPanel.add(generateButton);

        // Add sub-panels to main panel
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(storyScrollPane, BorderLayout.CENTER);
        mainPanel.add(eastPanel, BorderLayout.EAST);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        // Set margins between components using empty borders
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        frame.setContentPane(mainPanel);
        frame.pack();

        // frame.validate();
        // frame.setVisible(true);
    }

    private void setupButtonListener() {
        generateButton.addActionListener(e -> {
            String topic = inputField.getText();
            String creativityLevel = (String) this.creativityLevel.getSelectedItem();

            new SwingWorker<Void, String[]>() {
                private String story;

                @Override
                protected Void doInBackground() throws Exception {
                    System.out.println("Creativity Level: " + creativityLevel);

                    String modifiedTopic = modifyTopicAccordingToCreativity(topic, creativityLevel);

                    System.out.println("Modified Topic: " + modifiedTopic);

                    String story = generateStoryOnTopic(modifiedTopic);

                    String storyPlusTopic = "Topic: " + modifiedTopic + "\n\n" + story;

                    storyArea.setText(storyPlusTopic);


                    String[] imageDescriptions = generateImageDescriptionsFromStory(story);

                    for (String imageDescription : imageDescriptions) {
                        String imageURL = generateImageFromImageDescription(imageDescription);


                        String[] imageDetail = {
                                imageDescription,
                                imageURL
                        };
                        publish(imageDetail);
                    }
                    return null;
                }

                @Override
                protected void process(List<String[]> imageDetails) {
                    for (int i = 0; i < imageDetails.size(); i++) {
                        String[] imageDetail = imageDetails.get(i);
                        String imageDescription = imageDetail[0]; // of same image
                        String imageURL = imageDetail[1]; // of same image

                        imageCaption.setText(imageDescription);
                        loadImage(imageURL, i);
                    }
                }

                @Override
                protected void done() {
                    //do nothing
                    return;
                }
            }.execute();
        });
    }

    private static void writeImageToDisk(String imageUrl, String imagePath) {
        try {
            URL url = new URL(imageUrl);
            BufferedImage image = ImageIO.read(url);
            ImageIO.write(image, "jpeg", new File(imagePath));
            System.out.println("Image saved successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Load image from URL asynchronously
    private void loadImage(String imageUrl, int index) {
        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                try {
                    @SuppressWarnings("deprecation")
                    URL url = new URL(imageUrl);

                    String originalImagePath = baseDataPath + "original/image" + index + ".jpeg";
                    String editedImagePath = baseDataPath + "edited/image" + index + ".jpeg";

                    // Save to disk
                    writeImageToDisk(imageUrl, originalImagePath);

                    if (filterType.getSelectedItem().equals("Grayscale")) {
                        makeImageGrayscale(originalImagePath, editedImagePath);
                        System.out.println("Grayscale image saved successfully.");
                    } else if (filterType.getSelectedItem().equals("Sepia")) {
                        // Create a new matrix to store the sepia image
                        Mat sepiaImage = new Mat();
                        // Save the sepia image
                        String outputPath = "output/sepia_image.jpg";
                        Imgcodecs.imwrite(outputPath, sepiaImage);
                        System.out.println("Sepia image saved successfully.");
                    }

                    File displayImageFile = new File(filterType.getSelectedItem() != "None" ? editedImagePath : originalImagePath);
                    BufferedImage originalImage = ImageIO.read(displayImageFile);

                    // Specify the desired image size
                    int desiredWidth = 600; // Adjust width as needed
                    // int desiredHeight = (int) (originalImage.getHeight() * (desiredWidth / (double) originalImage.getWidth())); // Maintain aspect ratio
                    int desiredHeight = 600; // Adjust height as needed

                    // Create a new image of desired size
                    BufferedImage resizedImage = new BufferedImage(desiredWidth, desiredHeight, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = resizedImage.createGraphics();

                    // Apply quality rendering hints
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    // Draw the resized image
                    g2.drawImage(originalImage, 0, 0, desiredWidth, desiredHeight, null);
                    g2.dispose();

                    return new ImageIcon(resizedImage);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    ImageIcon imageIcon = get();
                    if (imageIcon != null) {
                        imageLabel.setIcon(imageIcon);
                        frame.pack();
                    } else {
                        System.out.println("Failed to load image from URL: " + imageUrl);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }


    // Creativity step: Modify the topic based on creativity level
    private static String modifyTopicAccordingToCreativity(String topic, String creativityLevel) throws JSONException {
        String API_KEY = "";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .method("POST", HttpRequest.BodyPublishers.ofString("{\n" +
                        "    \"model\": \"gpt-3.5-turbo\",\n" +
                        "    \"messages\": [\n" +
                        "      {\n" +
                        "        \"role\": \"system\",\n" +
                        "        \"content\": \"I will give a topic of a story and ask you to rewrite the topic according to the creativity level. Keep the essence of the story same or different according to the creativity level. For low level, do not make much changes. For high level, you must think outside the box. Do not write the story, just give me the updated topic according to the creativity level.\"\n"
                        +
                        "      },\n" +
                        "      {\n" +
                        "        \"role\": \"user\",\n" +
                        "        \"content\": \"Topic: " + topic + ", Creativity Level: " + creativityLevel + "\"\n"
                        +
                        "      }\n" +
                        "    ]\n" +
                        "}"))
                .build();

        HttpResponse<String> response = null;
        try {
            response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            return extractStoryContent(response.body());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null; // Consider handling the error more gracefully
        }
    }

    // Step 1: Generate Story based on user prompt.
    private static String generateStoryOnTopic(String topic) throws JSONException {
        String API_KEY = "";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .method("POST", HttpRequest.BodyPublishers.ofString("{\n" +
                        "                \"model\": \"gpt-3.5-turbo\",\n" +
                        "                \"messages\": [\n" +
                        "                        {\n" +
                        "                                \"role\": \"system\",\n" +
                        "                                \"content\": \"You an expert story write who will generate a short story on topic\"\n"
                        +
                        "                        },\n" +
                        "                        {\n" +
                        "                                \"role\": \"user\",\n" +
                        "                                \"content\": \"Topic: " + topic + "\"\n"
                        +
                        "                        }\n" +
                        "                ]\n" +
                        "}"))
                .build();

        HttpResponse<String> response = null;
        try {
            response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            return extractStoryContent(response.body());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null; // Consider handling the error more gracefully
        }
    }


    private static String extractStoryContent(String jsonResponse) throws JSONException {
        JSONObject jsonObj = new JSONObject(jsonResponse);
        JSONArray choices = jsonObj.optJSONArray("choices");
        if (choices == null || choices.length() == 0) {
            throw new JSONException("No choices in response");
        }
        JSONObject firstChoice = choices.getJSONObject(0);
        return firstChoice.getJSONObject("message").getString("content");
    }


    // Step 2: Generate Image Descriptions from the Story.
    private static String[] generateImageDescriptionsFromStory(String story) throws JSONException {
        String API_KEY = "";

        String normalizedStory = story.replace("\n", " ");
        System.out.println("Normalized Story: " + normalizedStory);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .method("POST", HttpRequest.BodyPublishers.ofString("{\n" +
                        "                \"model\": \"gpt-3.5-turbo\",\n" +
                        "                \"messages\": [\n" +
                        "                        {\n" +
                        "                                \"role\": \"system\",\n" +
                        "                                \"content\": \"I will give you a story on a topic and you will give me image descriptions, maximum 5, of the scene. Give output in JSON format of a list of strings, use JSON mode, don't use markdown\"\n"
                        +
                        "                        },\n" +
                        "                        {\n" +
                        "                                \"role\": \"user\",\n" +
                        "                                \"content\": \"Story: " + normalizedStory + "\"\n"
                        +
                        "                        }\n" +
                        "                ]\n" +
                        "}"))
                .build();

        HttpResponse<String> response = null;
        try {
            response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            System.out.println(responseBody);
            return extractImageDescriptions(responseBody);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null; // Consider handling the error more gracefully
        }
    }

    private static String[] extractImageDescriptions(String jsonResponse) throws JSONException {
        System.out.println("Input: " + jsonResponse);

        JSONObject jsonObj = new JSONObject(jsonResponse);
        String content = jsonObj.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");

        // Content is JSON array of strings, so let's parse and split it.
        content = content.replace("```json\n", "");
        content = content.replace("\n```", "");

        JSONArray contentArr = new JSONArray(content);
        String[] descriptions = new String[contentArr.length()];
        for (int i = 0; i < contentArr.length(); i++) {
            descriptions[i] = contentArr.getString(i);
        }
        return descriptions;
    }

    // Step 3: Generate Image from the Image Description.
    // Input: Image Description
    // Output: URL at which Dalle3 generates the image.
    private static String generateImageFromImageDescription(String imageDescription) throws JSONException {
        String API_KEY = "";

        String body = "{\"model\": \"dall-e-3\", \"prompt\":\"" + imageDescription + "\",\"size\":\"1024x1024\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/images/generations"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .method("POST", HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = null;
        try {
            response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            System.out.println(responseBody);

            return extractImageURL(responseBody);
        } catch (IOException |
                 InterruptedException e) {
            e.printStackTrace();
            return null; // Consider handling the error more gracefully
        }
    }

    private static String extractImageURL(String jsonResponse) throws JSONException {
        JSONObject jsonObj = new JSONObject(jsonResponse);
        String content = jsonObj.getJSONArray("data")
                .getJSONObject(0)
                .getString("url");
        return content;
    }

    private static void makeImageGrayscale(String srcPath, String dstPath) {
        // Load the OpenCV library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Read the input image
        Mat image = Imgcodecs.imread(srcPath);

        // Check if the image is loaded successfully
        if (image.empty()) {
            System.out.println("Failed to load the image.");
            return;
        }

        // Create a new matrix to store the grayscale image
        Mat grayImage = new Mat();

        // Convert the image to grayscale
        Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);

        // Save the grayscale image
        Imgcodecs.imwrite(dstPath, grayImage);

        System.out.println("Grayscale image saved successfully.");
    }


    public static void main(String[] args) throws JSONException {
        // Schedule a job for the event dispatch thread
        SwingUtilities.invokeLater(Main::new); // Create and show the GUI


        // String topic = "A story about a girl walking down the street";

        // String story = generateStoryOnTopic(topic);

        // System.out.println(story);

        System.out.println(System.getProperty("user.dir"));

        // Step 1: Generate Story based on user prompt.
//        String topic = "a very short girl is a very great basketball player and everyone is surprised";
//        String story = generateStoryOnTopic(topic);
//        System.out.println("Story: " + story);
//
//        // Step 2: Generate Image Descriptions from the Story.
//        String[] imageDescriptions = generateImageDescriptionsFromStory(story);
//
//        // Step 3: Generate Image from the Image Description.
//        for (String imageDescription : imageDescriptions) {
//            System.out.println("Image Description: " + imageDescription);
//            String imageResponse = generateImageFromImageDescription(imageDescription);
//            System.out.println(imageResponse);
//        }

        // String imagePath = "image/test.jpeg";
//        System.out.println(new File().getAbsolutePath());

        // makeImageGrayscale(imagePath);

//        System.out.println("Hello World" + Core.VERSION);

    }
}

