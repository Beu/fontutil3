package jp.beu.fontutil3;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;


public class FontUtil {

    public static class BlockInfo {

        	public int first;
        	public int last;
        	public String name;

        	public BlockInfo() {
        	}

        	public BlockInfo(int first, int last, String name) {
            this.first = first;
            this.last = last;
            this.name = name;
        	}

        	public static List<BlockInfo> getBlockInfoList() throws IOException {
            try (InputStream is = Main.class.getClassLoader().getResourceAsStream("Blocks.txt");
                    InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(isr)) {
                Pattern pattern = Pattern.compile("^([0-9A-F]+)\\.\\.([0-9A-F]+); (.+)$");
                return br.lines()
                        .map((String line) -> line.trim())
                        .map((String line) -> pattern.matcher(line))
                        .filter((Matcher matcher) -> matcher.matches())
                        .map((Matcher matcher) -> {
                            BlockInfo blockInfo = new BlockInfo();
                            blockInfo.first = Integer.parseInt(matcher.group(1), 16);
                            blockInfo.last = Integer.parseInt(matcher.group(2), 16);
                            blockInfo.name = matcher.group(3);
                            return blockInfo;
                        })
                        .collect(Collectors.toList());
            }
        	}

        	public static final String[] CJKUI_BLOCK_NAMES = {
                "CJK Radicals Supplement",
                "Kangxi Radicals",
                	"Ideographic Description Characters",
                "CJK Strokes",
                "CJK Compatibility",
                "CJK Unified Ideographs Extension A",
                "CJK Unified Ideographs",
                "CJK Compatibility Ideographs",
                "CJK Unified Ideographs Extension B",
                "CJK Unified Ideographs Extension C",
                "CJK Unified Ideographs Extension D",
                "CJK Unified Ideographs Extension E",
                "CJK Unified Ideographs Extension F",
        };

        public static final String[] PURE_CJKUI_BLOCK_NAMES = {
                "CJK Unified Ideographs Extension A",
                "CJK Unified Ideographs",
                "CJK Unified Ideographs Extension B",
                "CJK Unified Ideographs Extension C",
                "CJK Unified Ideographs Extension D",
                "CJK Unified Ideographs Extension E",
                "CJK Unified Ideographs Extension F",
        };

        public static List<BlockInfo> getCJKUIBlockInfoList() throws IOException {
            return getBlockInfoList().stream()
                    .filter((BlockInfo blockInfo) -> {
                        return Stream.of(CJKUI_BLOCK_NAMES)
                                .filter((String name) -> name.equals(blockInfo.name))
                                .findAny()
                                .isPresent();
                    })
                    .collect(Collectors.toList());
        }

        public static List<BlockInfo> getPureCJKUIBlockInfoList() throws IOException {
            return getBlockInfoList().stream()
                    .filter((BlockInfo blockInfo) -> {
                        return Stream.of(PURE_CJKUI_BLOCK_NAMES)
                                .filter((String name) -> name.equals(blockInfo.name))
                                .findAny()
                                .isPresent();
                    })
                    .sorted((BlockInfo o1, BlockInfo o2) -> o1.name.compareTo(o2.name))
                    .collect(Collectors.toList());
        }

        public static class WithDisplayables extends BlockInfo {

            public boolean[] displayables;

            public WithDisplayables(BlockInfo blockInfo) {
                super(blockInfo.first, blockInfo.last, blockInfo.name);
                displayables = new boolean[blockInfo.last + 1 - blockInfo.first];
            }

            public void printDisplayables() throws IOException {
                FileOutputStream fos = new FileOutputStream(FileDescriptor.out);
                OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                BufferedWriter bw = new BufferedWriter(osw);
                // don't close fos, osw, bw
                bw.write("-\n");
                bw.write(String.format("  name: %s\n", name));
                bw.write(String.format("  first: U+%04X\n", first));
                bw.write(String.format("  last: U+%04X\n", last));
                bw.write("  displayables:\n");
                String displayableString = "";
                for (int codePoint = first;  codePoint <= last;  ++codePoint) {
                    if ((codePoint & 0xff) == 0x00) {
                        if (!displayableString.isEmpty()) {
                            int startCodePoint = codePoint - displayableString.length();
                            if (displayableString.matches("^0+$")) {
                                displayableString = "0";
                            } else if (displayableString.matches("^1+$")) {
                                displayableString = "1";
                            }
                            bw.write(String.format("    - {0x%04X: %s}\n", startCodePoint, displayableString));
                            displayableString = "";
                        }
                    }
                    displayableString += displayables[codePoint - first] ? "1" : "0";
                }
                bw.flush();
            }

            public void printDisplayables(File file) throws IOException {
                try (FileOutputStream fos = new FileOutputStream(file, /*append:*/true);
                        OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                        BufferedWriter bw = new BufferedWriter(osw)) {
                    bw.write("-\n");
                    bw.write(String.format("  name: %s\n", name));
                    bw.write(String.format("  first: U+%04X\n", first));
                    bw.write(String.format("  last: U+%04X\n", last));
                    bw.write("  displayables:\n");
                    String displayableString = "";
                    for (int codePoint = first;  codePoint <= last;  ++codePoint) {
                        if ((codePoint & 0xff) == 0x00) {
                            if (!displayableString.isEmpty()) {
                                int startCodePoint = codePoint - displayableString.length();
                                if (displayableString.matches("^0+$")) {
                                    displayableString = "0";
                                } else if (displayableString.matches("^1+$")) {
                                    displayableString = "1";
                                }
                                bw.write(String.format("    - {0x%04X: %s}\n", startCodePoint, displayableString));
                                displayableString = "";
                            }
                        }
                        displayableString += displayables[codePoint - first] ? "1" : "0";
                    }
                    bw.flush();
                }
            }
        }
    }

    public static class Metrics {
        	public double width;
        	public double height;
        	public double ascent;
        	public double descent;

        	public static Metrics getMetrics(int codePoint, Font font, int fontSize) {
            Metrics metrics = new Metrics();
            Text text = new Text(new String(new int[] {codePoint}, 0, 1));
            text.setFont(font);
            metrics.width = text.getBoundsInLocal().getWidth();
            metrics.height = text.getBoundsInLocal().getHeight();
            metrics.ascent = text.getBaselineOffset();
            metrics.descent = text.getBoundsInLocal().getMaxY();
            return metrics;
        	}
    }

    public static boolean isWhite(Canvas canvas) {
        WritableImage writableImage = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
        SnapshotParameters parameters = new SnapshotParameters();
        canvas.snapshot(parameters, writableImage);
        PixelReader pr = writableImage.getPixelReader();
        Loop: while (true) {
            for (int y = 0;  y < (int) canvas.getHeight();  ++y) {
                for (int x = 0;  x < (int) canvas.getWidth();  ++x) {
                    Color color = pr.getColor(x, y);
                    if (!color.equals(Color.WHITE)) {
                        // is not white
                        break Loop;
                    }
                }
            }
            // all white
            return true;
        }
        return false;
    }
}
