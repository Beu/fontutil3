package jp.beu.fontutil3;

import java.awt.FontFormatException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;


public class Main extends Application {

    public static void main(String[] arguments) {
	Application.launch(arguments);
    }

    private Stage mainStage;
    private Scene mainScene;
    private FlowPane mainPane;
    private final Deque<Thread> threadDeque = new LinkedBlockingDeque<>();

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("start");
        mainStage = primaryStage;
        mainStage.setTitle("fontutil3");
        {
            mainScene = createMainScene();
            mainStage.setScene(mainScene);
            mainStage.sizeToScene();
        }
        mainStage.setOnCloseRequest((WindowEvent event) -> {
            if (confirmQuitting()) {
                Platform.exit();
            } else {
                event.consume();  // event 發火を 撤回す
            }
        });
        mainStage.show();
    }

    @Override
    public void stop() throws Exception {
        System.out.println("stop");
        while (!threadDeque.isEmpty()) {
            Thread thread = threadDeque.pop();
            thread.interrupt();
        }
        mainStage.hide();
        mainStage.close();
    }

    private boolean confirmQuitting() {
        Alert alert = new Alert(AlertType.CONFIRMATION, "Are you sure?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("confirm quitting");
        alert.setHeaderText("You will quit this application.");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }

    private Scene createMainScene() {
        VBox vBox = new VBox();
        vBox.setPrefSize(800.0d, 600.0d);
        {
            MenuBar menuBar = new MenuBar();
            menuBar.getMenus().addAll(
                    createFileMenu(),
                    createHelpMenu());
                    vBox.getChildren().add(menuBar);
        }
        {
            AnchorPane anchorPane = new AnchorPane();
//          VBox.setVgrow(anchorPane, Priority.ALWAYS);
            {
                ScrollPane scrollPane = new ScrollPane();
                scrollPane.setFitToWidth(true);
                scrollPane.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
                scrollPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
                AnchorPane.setLeftAnchor(scrollPane, 0.0d);
                AnchorPane.setTopAnchor(scrollPane, 0.0d);
                AnchorPane.setRightAnchor(scrollPane, 0.0d);
                AnchorPane.setBottomAnchor(scrollPane, 0.0d);
                {
                    AnchorPane anchorPane2 = new AnchorPane();
                    {
                        mainPane = new FlowPane();
                        mainPane.setStyle("-fx-border-style:solid; -fx-border-width:1; -fx-border-color:red;");
                        mainPane.setPrefSize(800.0d, 600.0d);
                        mainPane.setHgap(4.0d);
                        mainPane.setVgap(4.0d);
                        mainPane.setOrientation(Orientation.HORIZONTAL);
                        mainPane.setPadding(new Insets(4.0d));
                        AnchorPane.setLeftAnchor(mainPane, 0.0d);
                        AnchorPane.setTopAnchor(mainPane, 0.0d);
                        AnchorPane.setRightAnchor(mainPane, 0.0d);
                        AnchorPane.setBottomAnchor(mainPane, 0.0d);

                        anchorPane2.getChildren().add(mainPane);
                    }
                    scrollPane.setContent(anchorPane2);
                }
                anchorPane.getChildren().add(scrollPane);
            }
            vBox.getChildren().add(anchorPane);
        }
        Scene scene = new Scene(vBox, 800.0d, 600.0d);
        return scene;
    }

    private Menu createFileMenu() {
        Menu menu = new Menu("File");
        {
            MenuItem item = new MenuItem("Open font files...");
            item.setOnAction((ActionEvent event) -> {
                FileChooser chooser = new FileChooser();
                chooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("font files", "*.ttf", "*.ttc", "*.otf", "*.otc"),
                        new FileChooser.ExtensionFilter("all files", "*.*"));
                List<File> fileList = chooser.showOpenMultipleDialog(mainStage);
                if (fileList != null && fileList.size() > 0) {
                    processFontFiles(fileList);
                }
            });
            menu.getItems().add(item);
        }
        {
            MenuItem item = new MenuItem("Close all");
            item.setOnAction((ActionEvent event) -> {
                mainPane.getChildren().removeAll(mainPane.getChildren());
                while (!threadDeque.isEmpty()) {
                    Thread thread = threadDeque.pop();
                    thread.interrupt();
                }
            });
            menu.getItems().add(item);
        }
        menu.getItems().add(new SeparatorMenuItem());
        {
            MenuItem item = new MenuItem("Quit...");
            item.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.ALT_DOWN));
            item.setOnAction((ActionEvent event) -> {
                if (confirmQuitting()) {
                    Platform.exit();
                }
            });
            menu.getItems().add(item);
        }
        return menu;
    }

    private Menu createHelpMenu() {
        Menu menu = new Menu("Help");
        {
            MenuItem item = new MenuItem("About...");
            item.setOnAction((ActionEvent event) -> {
                Alert alert = new Alert(AlertType.INFORMATION, "", ButtonType.CLOSE);
                alert.setTitle("About...");
                alert.setHeaderText("What is this?");
                alert.showAndWait();
            });
            menu.getItems().add(item);
        }
        return menu;
    }

    public static double CANVAS_SIZE = 256.0d;
    public static double FONT_SIZE = 256.0d - 16.0d;

    private void processFontFiles(List<File> fileList) {
        for (File file : fileList) {
            java.awt.Font[] awtFonts;
            Font[] fonts;
            try {
                awtFonts = java.awt.Font.createFonts(new FileInputStream(file));
                fonts = Font.loadFonts(new FileInputStream(file), FONT_SIZE);
                if (awtFonts.length != fonts.length) {
                    throw new FontFormatException("There is a different between java.awt.Font and javafx.scene.text.Font");
                }
                if (awtFonts.length == 0) {
                    throw new FontFormatException(file.getName() + " is not a font file");
                }
            } catch (IOException | FontFormatException e) {
                Alert alert = new Alert(AlertType.ERROR, "", ButtonType.CLOSE);
                alert.setTitle("error");
                alert.setHeaderText(e.getMessage());
                continue;
            }
            VBox vBox = new VBox();
            vBox.setSpacing(4.0d);
            vBox.setPadding(new Insets(4.0d));
            vBox.setStyle("-fx-border-style:solid; -fx-border-width:1;");
            {
                Label label = new Label(file.getName());
                vBox.getChildren().add(label);
            }
            {
                HBox hBox = new HBox();
                hBox.setSpacing(4.0d);
                hBox.setPadding(new Insets(4.0d));
                hBox.setStyle("-fx-border-style:solid; -fx-border-widdth:1;");
                for (int i = 0;  i < awtFonts.length;  ++i) {
                    VBox vBox2 = new VBox();
                    vBox2.setPadding(new Insets(4.0d));
                    vBox2.setSpacing(4.0d);
                    vBox2.setStyle("-fx-border-style:solid; -fx-border-widdth:1;");
                    {
                        vBox2.getChildren().add(new Text("awt family: " + awtFonts[i].getFamily()));
                        vBox2.getChildren().add(new Text("awt font name: " + awtFonts[i].getFontName()));
                        vBox2.getChildren().add(new Text("family: " + fonts[i].getFamily()));
                        vBox2.getChildren().add(new Text("name: " + fonts[i].getName()));
                    }
                    {
                        Canvas canvas = new Canvas(CANVAS_SIZE, CANVAS_SIZE);
                        {
                            GraphicsContext context = canvas.getGraphicsContext2D();
                            context.setFill(Color.WHITE);
                            context.fillRect(0.0d, 0.0d, CANVAS_SIZE, CANVAS_SIZE);
                        }
                        vBox2.getChildren().add(canvas);
                    }
                    {
                        HBox hBox2 = new HBox();
                        hBox2.setAlignment(Pos.CENTER);
                        hBox2.setSpacing(4.0d);
                        {
                            final Button button = new Button("test1");
                            final int fI = i;
                            final Canvas canvas = (Canvas) vBox2.getChildren().stream()
                                    .filter((Node node) -> node instanceof Canvas)
                                    .findFirst().get();
                            assert canvas != null;

                            hBox2.getChildren().add(button);

                            final ProgressBar progressBar = new ProgressBar(0.0d);
                            progressBar.autosize();
                            hBox2.getChildren().add(progressBar);

                            button.setOnAction((ActionEvent event) -> {
                                button.setDisable(true);
                                test1(canvas, file, awtFonts[fI], fonts[fI], (Double percentage) -> {
                                    progressBar.setProgress(percentage);
                                    return;
                                }, (List<FontUtil.BlockInfo.WithDisplayables> displayablesList) -> {
                                    for (FontUtil.BlockInfo.WithDisplayables displayables: displayablesList) {
                                        try {
                                            displayables.printDisplayables();
                                        } catch (IOException ex) {
                                            ex.printStackTrace();
                                        }
                                    }
                                    Platform.runLater(() -> {
                                        FileChooser chooser = new FileChooser();
                                        chooser.setTitle("Write YAML File");
                                        chooser.getExtensionFilters().addAll(
                                                new FileChooser.ExtensionFilter("yaml files", "*.yml"),
                                                new FileChooser.ExtensionFilter("all files", "*.*"));
                                        File yamlFile = chooser.showSaveDialog(mainStage);
                                        if (yamlFile != null) {
                                            for (FontUtil.BlockInfo.WithDisplayables displayables: displayablesList) {
                                                try {
                                                    displayables.printDisplayables(yamlFile);
                                                } catch (IOException ex) {
                                                    ex.printStackTrace();
                                                    yamlFile.delete();
                                                    throw new RuntimeException(ex);
                                                }
                                            }
                                        }
                                    });
                                });
                            });
                        }
                        vBox2.getChildren().add(hBox2);
                    }
                    hBox.getChildren().add(vBox2);
                }
                vBox.getChildren().add(hBox);
            }
            mainPane.getChildren().add(vBox);
        }
        System.out.println("processFontFiles ends.");
    }

    private void test1(Canvas canvas, File fontFile, java.awt.Font awtFont, Font font, Consumer<Double> onProgress, Consumer<List<FontUtil.BlockInfo.WithDisplayables>> onComplete) {
        System.out.println("test1 starts.");
        try {
            Thread thread = new Thread(new Test1Task(canvas, fontFile, awtFont, font, onProgress, onComplete));
            threadDeque.add(thread);
            thread.start();
        } catch (InterruptedException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private class Test1Task implements Runnable {

        private final Canvas canvas;
        private final File fontFile;
        private final java.awt.Font awtFont;
        private final Font font;
        private final Consumer<Double> onProgress;
        private final Consumer<List<FontUtil.BlockInfo.WithDisplayables>> onComplete;

        public Test1Task(Canvas canvas, File fontFile, java.awt.Font awtFont, Font font, Consumer<Double> onProgress, Consumer<List<FontUtil.BlockInfo.WithDisplayables>> onComplete) throws InvocationTargetException, InterruptedException {
            System.out.println("Test1Task() starts.");
            this.canvas = canvas;
            this.fontFile = fontFile;
            this.awtFont = awtFont;
            this.font = font;
            this.onProgress = onProgress;
            this.onComplete = onComplete;

            System.out.println("TestTask() ends.");
        }

        private Thread thread;
        private int maxOfFonts;
        private int nOfFonts;

        @Override
        public void run() {
            System.out.println("call starts.");
            thread = Thread.currentThread();
            List<FontUtil.BlockInfo.WithDisplayables> blockInfoWithDisplayablesList;
            try {
                maxOfFonts = 0;
                nOfFonts = 0;
                blockInfoWithDisplayablesList = FontUtil.BlockInfo.getPureCJKUIBlockInfoList().stream()
                        .map((FontUtil.BlockInfo blockInfo) -> {
                            maxOfFonts += blockInfo.last + 1 - blockInfo.first;
                            return new FontUtil.BlockInfo.WithDisplayables(blockInfo);
                        })
                        .collect(Collectors.toList());
                onProgress.accept((double) nOfFonts / (double) maxOfFonts);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Loop starts.");
            Loop: for (FontUtil.BlockInfo.WithDisplayables blockInfoWithDisplayables : blockInfoWithDisplayablesList) {
                System.out.println(blockInfoWithDisplayables.name + " starts.");
                CountDownLatch countDownLatch = new CountDownLatch(blockInfoWithDisplayables.last + 1 - blockInfoWithDisplayables.first);
                for (int codePoint = blockInfoWithDisplayables.first;  codePoint <= blockInfoWithDisplayables.last;  ++codePoint) {
                    final int fCodePoint = codePoint;
                    if (!awtFont.canDisplay(fCodePoint)) {
                        blockInfoWithDisplayables.displayables[fCodePoint - blockInfoWithDisplayables.first] = false;
                        countDownLatch.countDown();
                        ++nOfFonts;
                        onProgress.accept((double) nOfFonts / (double) maxOfFonts);
                        continue;
                    }

                    Platform.runLater(() -> {
                        try {
                            FontUtil.Metrics metrics = FontUtil.Metrics.getMetrics(fCodePoint, font, (int) FONT_SIZE);

                            GraphicsContext context = canvas.getGraphicsContext2D();
                            context.setFill(Color.WHITE);
                            context.fillRect(0.0, 0.0, canvas.getWidth(), canvas.getHeight());
                            context.setFill(Color.BLACK);
                            context.setFont(font);
                            context.setFontSmoothingType(FontSmoothingType.GRAY);
                            context.fillText(
                                    new String(new int[] {fCodePoint}, 0, 1),
                                    (canvas.getWidth() - metrics.width) / 2.0d,
                                    (canvas.getHeight() - metrics.height) / 2.0d + metrics.ascent);
//                            Main.this.mainStage.show();

                            if (blockInfoWithDisplayables.displayables[fCodePoint - blockInfoWithDisplayables.first] = !FontUtil.isWhite(canvas)) {
                                // displayable
                                // 折角なれば 軸を畫かむ
                                // y-axis
                                context.setStroke(Color.GRAY);
                                context.setLineWidth(1.0d);
                                context.strokeLine(
                                        (canvas.getWidth() - metrics.width) / 2.0d,
                                        0.0d,
                                        (canvas.getWidth() - metrics.width) / 2.0d,
                                        canvas.getHeight());
                                context.strokeLine(
                                        (canvas.getWidth() - metrics.width) / 2.0d + metrics.width,
                                        0.0d,
                                        (canvas.getWidth() - metrics.width) / 2.0d + metrics.width,
                                        canvas.getHeight());
                                context.strokeLine(
                                        0.0d,
                                        (canvas.getHeight() - metrics.height) / 2.0d,
                                        canvas.getWidth(),
                                        (canvas.getHeight() - metrics.height) / 2.0d);
                                // baseline
                                context.strokeLine(
                                        0.0d,
                                        (canvas.getHeight() - metrics.height) / 2.0d + metrics.ascent,
                                        canvas.getWidth(),
                                        (canvas.getHeight() - metrics.height) / 2.0d + metrics.ascent);
                                // ascent
                                context.strokeLine(
                                        0.0d,
                                        (canvas.getHeight() - metrics.height) / 2.0d,
                                        canvas.getWidth(),
                                        (canvas.getHeight() - metrics.height) / 2.0d);
                                // descent
                                context.strokeLine(
                                        0.0d,
                                        (canvas.getHeight() - metrics.height) / 2.0d + metrics.ascent + metrics.descent,
                                        canvas.getWidth(),
                                        (canvas.getHeight() - metrics.height) / 2.0d + metrics.ascent + metrics.descent);
//                                Main.this.mainStage.show();
                            } else {
                                // not displayable
                                System.out.println(String.format("U+%04X is white.", fCodePoint));
                            }
                        } finally {
                            countDownLatch.countDown();
                            ++nOfFonts;
                            onProgress.accept((double) nOfFonts / (double) maxOfFonts);
                        }
                    });
                    try {
                        Thread.sleep(20L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break Loop;
                    }
                }
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break Loop;
                }
            }
            threadDeque.remove(thread);
            System.out.println("Loop ends.");
            onComplete.accept(blockInfoWithDisplayablesList);
        }
    }

}

