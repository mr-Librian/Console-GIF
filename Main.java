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

    static boolean ascii = false;
    static boolean reply = false;
    static final String usage = """
                usage: java Main <URI/URL>
                                        
                options:
                    --width <value>                   set image width(chars)
                    --height <value>                  set image width(chars)
                    --scale_x <value>                 set image x scale
                    --scale_y <value>                 set image y scale
                    --size <width, height>            set image width and height
                    --repeat <true/false>             reply gif after end
                    --ascii <256-char palette>        output ascii art with defined palette
                """;

    public static void main(String[] args) throws URISyntaxException, IOException {
        parseArgs(args);

        try {
            System.out.println("Splitting gif");
            for (int v = 0; v < reader.getNumImages(true); v++) {
                NamedNodeMap attributes = reader.getImageMetadata(v).getAsTree("javax_imageio_gif_image_1.0").getChildNodes().item(0).getAttributes();
                thumb.getGraphics().drawImage(reader.read(v), Integer.parseInt(attributes.getNamedItem("imageLeftPosition").getNodeValue()), Integer.parseInt(attributes.getNamedItem("imageTopPosition").getNodeValue()), null);

                BufferedImage image = new BufferedImage((int) (thumb.getWidth() * scaleX), (int) (thumb.getHeight() * scaleY), BufferedImage.TYPE_INT_ARGB);
                AffineTransform scale = new AffineTransform();
                scale.scale(scaleX, scaleY);
                image = new AffineTransformOp(scale, AffineTransformOp.TYPE_BILINEAR).filter(thumb, image);

                parseImage(image);
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
            System.out.println("Loading image");
            thumb = ImageIO.read(new URI(a[0]).toURL());
            reader.setInput(ImageIO.createImageInputStream(new URI(a[0]).toURL().openStream()));
        } else if (a[0].equals("--help") || a[0].equals("-h") || a[0].equals("-help")) {
            System.out.println(usage);
            System.exit(0);
        } else {
            System.out.println("Loading image");
            thumb = ImageIO.read(new File(a[0]));
            reader.setInput(ImageIO.createImageInputStream(new File(a[0])));
        }
        for (int i = 1; i < a.length; i++) {
            switch (a[i]) {
                case "--width" -> scaleX = (double) Integer.parseInt(a[i + 1]) / thumb.getWidth();
                case "--height" -> scaleY = (double) Integer.parseInt(a[i + 1]) / thumb.getHeight();
                case "--scale_x" -> scaleX = Double.parseDouble(a[i + 1]);
                case "--scale_y" -> scaleY = Double.parseDouble(a[i + 1]);
                case "--size" -> {
                    scaleX = (double) Integer.parseInt(a[i + 1]) / thumb.getWidth();
                    scaleY = (double) Integer.parseInt(a[i + 2]) / thumb.getHeight();
                }
                case "--repeat" -> reply = true;
                case "--ascii" -> {
                    ascii = true;
                    palette = a[i + 1];
                }
                case "-h", "-help", "--help" -> {
                    System.out.println(usage);
                    System.exit(0);
                }
            }
        }
    }

    static void parseImage(BufferedImage image) {
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
}
