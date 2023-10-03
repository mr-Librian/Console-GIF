import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    static final ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
    static List<String> output = new ArrayList<>();
    static BufferedImage thumb;
    static String palette = "";

    static double scaleX = 1;
    static double scaleY = 1;
    static double pixel = 0.6;

    static boolean ascii = false;
    static boolean autoScale = true;
    static boolean reply = false;

    public static void main(String[] args) throws URISyntaxException, IOException {
        parseArgs(args);

        try {
            for (int v = 0; v < reader.getNumImages(true); v++) {
                NamedNodeMap attributes = reader.getImageMetadata(v).getAsTree("javax_imageio_gif_image_1.0").getChildNodes().item(0).getAttributes();
                thumb.getGraphics().drawImage(reader.read(v), Integer.parseInt(attributes.getNamedItem("imageLeftPosition").getNodeValue()), Integer.parseInt(attributes.getNamedItem("imageTopPosition").getNodeValue()), null);

                BufferedImage image = new BufferedImage((int) (thumb.getWidth() * scaleX), (int) (thumb.getHeight() * scaleY), BufferedImage.TYPE_INT_ARGB);
                AffineTransform scale = new AffineTransform();
                scale.scale(scaleX, scaleY);
                image = new AffineTransformOp(scale, AffineTransformOp.TYPE_BILINEAR).filter(thumb, image);


                StringBuilder builder = new StringBuilder();

                int width = image.getWidth();
                int height = image.getHeight();

                for (int i = 0; i < height; i++) {
                    for (int j = 0; j < width; j++) {
                        int rgb = image.getRGB(j, i);
                        int alpha = ((rgb & 0xff000000) >>> 24) / 255;
                        int red = (rgb & 0x00ff0000) >>> 16;
                        int green = (rgb & 0x0000ff00) >>> 8;
                        int blue = rgb & 0x000000ff;

                        var x = "%s".formatted(ascii ? palette.charAt((red + green + blue) / 3 * alpha) : "\033[38;2;%s;%s;%sm#".formatted(red, green, blue));
                        builder.append(x);

                    }
                    builder.append('\n');
                }

                output.add(builder.toString());
            }
            PrintWriter writer = new PrintWriter(System.out, false);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                writer.write("\033[0;0m");
                writer.close();
            }));
            do {
                output.forEach(e -> {
                    writer.write("\033[H");
                    writer.write(e);
                    writer.flush();
                    try {
                        Thread.sleep(42);
                    } catch (InterruptedException ignore) {
                    }
                });
            } while (reply);
        } catch (IOException | DOMException | NumberFormatException e) {
            throw new RuntimeException(e);
        }
    }

    static void parseArgs(String[] a) throws URISyntaxException, IOException {
        if (a[0].startsWith("http")) {
            thumb = ImageIO.read(new URI(a[0]).toURL());
            reader.setInput(ImageIO.createImageInputStream(new URI(a[0]).toURL().openStream()));
        } else {
            thumb = ImageIO.read(new File(a[0]));
            reader.setInput(ImageIO.createImageInputStream(new File(a[0])));
        }
        for (int i = 1; i < a.length; i++) {
            switch (a[i]) {
                case "--auto_scale" -> autoScale = Boolean.parseBoolean(a[i + 1]);
                case "--width" -> scaleX = (double) Integer.parseInt(a[i + 1]) / thumb.getWidth();
                case "--height" -> scaleY = (double) Integer.parseInt(a[i + 1]) / thumb.getHeight();
                case "--scale_x" -> scaleX = Double.parseDouble(a[i + 1]);
                case "--scale_y" -> scaleY = Double.parseDouble(a[i + 1]);
                case "--pixel" -> pixel = Double.parseDouble(a[i + 1]);
                case "--reply" -> reply = true;
                case "--ascii" -> {
                    ascii = true;
                    palette = a[i + 1];
                }
            }
        }
        if (scaleX == 1 && autoScale) scaleX = scaleX * scaleY * pixel;
        else if (scaleY == 1 && autoScale) scaleY = scaleY * scaleX * pixel;
    }
}
