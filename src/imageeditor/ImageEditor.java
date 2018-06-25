/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imageeditor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import java.util.Arrays;
import java.util.HashMap;

/**
 *
 * @author sylvester
 */
public class ImageEditor {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        //fill in variables with path to files
        String PATH_TO_FOLDER_WITH_BE_SPLIT = "";
        String PATH_TO_MAIN_FILE_TO_BE_SPLIT = "";
        
        
        
        File folder = new File(PATH_TO_FOLDER_WITH_BE_SPLIT);
        File[] listOfFiles = folder.listFiles();

        //temporary folder for storing split images
        File folder2 = new File(".tmp");
        File[] listOfTmpFiles = folder2.listFiles();

        File mainImage = new File(PATH_TO_MAIN_FILE_TO_BE_SPLIT);
        int rows = 20;
        int cols = 20;
        splitImage(mainImage, rows, cols, listOfFiles);
    }

    public static void splitImage(File file, int rows, int cols, File listOfFiles[]) {
        FileInputStream fis = null;
        BufferedImage bi = null;
        try {
            System.out.println("Starting ....");
            System.out.println("Reading image ....");
            fis = new FileInputStream(file);
            bi = ImageIO.read(fis);

            int tiles = rows * cols;
            final int tileWidth = bi.getWidth() / cols;
            final int tileHeight = bi.getHeight() / rows;
            int count = 0;

            BufferedImage newImage[] = new BufferedImage[tiles];
            System.out.println("spliting image ....");
            for (int x = 0; x < rows; x++) {
                for (int y = 0; y < cols; y++) {
                    newImage[count] = new BufferedImage(tileWidth, tileHeight, bi.getType());
                    Graphics2D g2d = newImage[count++].createGraphics();
                    g2d.drawImage(bi, 0, 0, tileWidth, tileHeight, tileWidth * y, tileHeight * x, tileWidth * y + tileWidth, tileHeight * x + tileHeight, null);
                    g2d.dispose();
                }
            }

            new File(".tmp").mkdirs();
            for (int i = 0; i < newImage.length; i++) {
                ImageIO.write(newImage[i], "jpg", new File(".tmp", "split" + i + ".jpg"));
            }

            File folder2 = new File(".tmp");
            File[] listOfTmpFiles22 = folder2.listFiles();

            System.out.println("calcualting colour differences and replacing images ....");
            Map<String, float[]> splitImagesLab = getAvgColorLab(listOfTmpFiles22);
            Map<String, float[]> allImages = getAvgColorLab(listOfFiles);
            splitImagesLab.forEach((k, v) -> {
                try {
                    Map<String, Double> tempDiffArray = new HashMap<>();
                    allImages.forEach((k2, v2) -> {
                        double deltaE = Math.sqrt(Math.pow((v[0] - v2[0]), 2) + Math.pow((v[1] - v2[1]), 2) + Math.pow((v[2] - v2[2]), 2));
                        tempDiffArray.put(k2, deltaE);
                    });
                    Map.Entry<String, Double> min
                            = tempDiffArray
                                    .entrySet()
                                    .stream()
                                    .min(
                                            Map.Entry.comparingByValue(Double::compareTo)
                                    )
                                    .get();

                    File mainImage = new File(min.getKey());
                    FileInputStream fis2 = new FileInputStream(mainImage);
                    BufferedImage bi2 = ImageIO.read(fis2);

                    BufferedImage outputSplitImage = new BufferedImage(tileWidth, tileHeight, bi2.getType());

                    // scales the input image to the output image
                    Graphics2D g2d = outputSplitImage.createGraphics();
                    g2d.drawImage(bi2, 0, 0, tileWidth, tileHeight, null);
                    g2d.dispose();
                    ImageIO.write(outputSplitImage, "jpg", new File(k));

                } catch (FileNotFoundException ex) {
                    Logger.getLogger(ImageEditor.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(ImageEditor.class.getName()).log(Level.SEVERE, null, ex);
                }

            });
            System.out.println("creating final image ....");
            BufferedImage result = new BufferedImage(
                    bi.getWidth(), bi.getHeight(), //work these out
                    BufferedImage.TYPE_INT_RGB);
            Graphics g = result.getGraphics();
            int z = 0;
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    FileInputStream fis3 = new FileInputStream(listOfTmpFiles22[z++]);
                    BufferedImage biFinal = ImageIO.read(fis3);
                    g.drawImage(biFinal, tileWidth * j, tileHeight * i, null);
                }
            }

            ImageIO.write(result, "jpg", new File("output", "finalImage.jpg"));
            System.out.println("Done ....");

        } catch (FileNotFoundException ex) {
            Logger.getLogger(ImageEditor.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ImageEditor.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fis.close();
            } catch (IOException ex) {
                Logger.getLogger(ImageEditor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static Map getAvgColorLab(File files[]) {
        Map<String, float[]> imageMap = null;
        try {
            imageMap = new HashMap<>();
            for (File file : files) {
                if (file.isFile()) {
                    FileInputStream fis = new FileInputStream(file);
                    BufferedImage bi = ImageIO.read(fis);
                    int w = bi.getWidth();
                    int h = bi.getHeight();
                    int x0 = bi.getMinX();
                    int y0 = bi.getMinY();
                    Color color = averageColor(bi, x0, y0, w, h);
                    float red = color.getRed();
                    float green = color.getGreen();
                    float blue = color.getBlue();
                    float[] colorArray = {red, green, blue};
                    CIELab lab = CIELab.getInstance();
                    float[] labF = lab.fromRGB(colorArray);
                    imageMap.put(file.getAbsolutePath(), labF);
                }

            }

        } catch (IOException ex) {
            Logger.getLogger(ImageEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
        return imageMap;
    }

    public static Color averageColor(BufferedImage bi, int x0, int y0, int w, int h) {
        int x1 = x0 + w;
        int y1 = y0 + h;
        int sumr = 0, sumg = 0, sumb = 0;
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                Color pixel = new Color(bi.getRGB(x, y));
                sumr += pixel.getRed();
                sumg += pixel.getGreen();
                sumb += pixel.getBlue();
            }
        }
        int num = w * h;
        return new Color(sumr / num, sumg / num, sumb / num);
    }

}

class CIELab extends ColorSpace {

    public static CIELab getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public float[] fromCIEXYZ(float[] colorvalue) {
        double l = f(colorvalue[1]);
        double L = 116.0 * l - 16.0;
        double a = 500.0 * (f(colorvalue[0]) - l);
        double b = 200.0 * (l - f(colorvalue[2]));
        return new float[]{(float) L, (float) a, (float) b};
    }

    @Override
    public float[] fromRGB(float[] rgbvalue) {
        float[] xyz = CIEXYZ.fromRGB(rgbvalue);
        return fromCIEXYZ(xyz);
    }

    @Override
    public float getMaxValue(int component) {
        return 128f;
    }

    @Override
    public float getMinValue(int component) {
        return (component == 0) ? 0f : -128f;
    }

    @Override
    public String getName(int idx) {
        return String.valueOf("Lab".charAt(idx));
    }

    @Override
    public float[] toCIEXYZ(float[] colorvalue) {
        double i = (colorvalue[0] + 16.0) * (1.0 / 116.0);
        double X = fInv(i + colorvalue[1] * (1.0 / 500.0));
        double Y = fInv(i);
        double Z = fInv(i - colorvalue[2] * (1.0 / 200.0));
        return new float[]{(float) X, (float) Y, (float) Z};
    }

    @Override
    public float[] toRGB(float[] colorvalue) {
        float[] xyz = toCIEXYZ(colorvalue);
        return CIEXYZ.toRGB(xyz);
    }

    CIELab() {
        super(ColorSpace.TYPE_Lab, 3);
    }

    private static double f(double x) {
        if (x > 216.0 / 24389.0) {
            return Math.cbrt(x);
        } else {
            return (841.0 / 108.0) * x + N;
        }
    }

    private static double fInv(double x) {
        if (x > 6.0 / 29.0) {
            return x * x * x;
        } else {
            return (108.0 / 841.0) * (x - N);
        }
    }

    private Object readResolve() {
        return getInstance();
    }

    private static class Holder {

        static final CIELab INSTANCE = new CIELab();
    }

    private static final long serialVersionUID = 5027741380892134289L;

    private static final ColorSpace CIEXYZ
            = ColorSpace.getInstance(ColorSpace.CS_CIEXYZ);

    private static final double N = 4.0 / 29.0;

}
