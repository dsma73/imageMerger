package com.silverwing;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class ImageMerger {

    private static final String CONFIG_PATH="config.cfg";
    private static final String DEFAULT_FONT = "휴먼둥근헤드라인";

    private String configPath;
    private JSONObject config;
    private boolean debug  = false;
    private String targetPath = "output";
    private String outputFormat = "jpg";
    private int textSize = 128;
    private int textBorder = 20;
    private int drawWidth = 0;
    private int drawHeight = 0;
    private JSONArray targets ;
    private String fontFace = DEFAULT_FONT;
    private boolean isText = true;
    private Color textColor = Color.orange;
    private boolean genVideo = true;

    private void log(Object log){
        System.out.println(log.toString());
    }

    public void loadConfig() throws  Exception {
        FileReader reader = new FileReader(this.configPath);

        JSONParser parser = new JSONParser();
        config = (JSONObject) parser.parse(reader);

        isText = "text".equalsIgnoreCase(getValue("mode"));

        if( isText ) {
            targets = (JSONArray) config.get("targets");
            if (targets == null || targets.size() == 0) {
                throw new Exception(" Target Value is mandatory");
            }

            try{
                String rgbs = config.get("fontColor").toString();
                if( rgbs == null )
                    return;

                StringTokenizer tok = new StringTokenizer(rgbs,",");

                int r = Integer.parseInt(tok.nextToken());
                int g = Integer.parseInt(tok.nextToken());
                int b = Integer.parseInt(tok.nextToken());

                textColor = new Color(r,g,b);
            }catch (Exception e){
                textColor = Color.orange;
            }
        }else{
            try {
                drawHeight = Integer.parseInt(config.getOrDefault("drawWidth", 200).toString());
                drawWidth = Integer.parseInt(config.getOrDefault("drawHeight", 200).toString());
            }catch (Exception e){}
        }

        try{
            debug = (Boolean)config.getOrDefault("debug",Boolean.FALSE);
        }catch (Exception e){}

        try{
            DateFormat dateFormat = new SimpleDateFormat("dd_HHmmss");
            targetPath = config.getOrDefault("outputDir",  dateFormat.format(new Date())).toString();
            File f = new File(targetPath);
            if( !f.exists()){
                f.mkdir();
            }
        }catch (Exception e){}

        try{
            outputFormat = config.getOrDefault("outputFormat","png").toString();
        }catch(Exception e){}

        try{
            textSize = Integer.parseInt( config.get("textSize").toString());
            textBorder = Integer.parseInt( config.get("textBorder").toString());
        }catch (Exception e){
            textSize = 128;
            textBorder = 24;
        }

        try{
            fontFace = config.getOrDefault("font",DEFAULT_FONT).toString();
        }catch (Exception e){}

        try{
            genVideo = (Boolean)config.getOrDefault("genVideo",Boolean.TRUE);
        }catch(Exception e){}

    }

    public String getValue(String key){
        Object val = config.get(key);
        if( val == null ){
            return null;
        }
        return val.toString();
    }

    public  Image makeColorTransparent(final Image im, final Color color) {
        final ImageFilter filter = new RGBImageFilter() {
            // the color we are looking for (white)... Alpha bits are set to opaque

            public final int filterRGB(final int x, final int y, final int rgb) {
                int gap = 60;
                try{
                    gap = Integer.parseInt(getValue("gap"));
                }catch (Exception e){
                }

                int R =  0xFF - ((rgb & 0x00FF0000) >> 16);
                int G =  0xFF - ((rgb & 0x0000FF00) >> 8);
                int B =  0xFF - ((rgb & 0x000000FF) );

                if (  R+G+B < gap * 3){
                    int alpha = (R+G+B);

                    return ((alpha << 24) | 0x00FFFFFF) & rgb;
                }

                else {
                    if( debug ) {
                        log(" R " + ((rgb & 0x00FF0000) >> 16) + " G " + ((rgb & 0x0000FF00) >> 8) + " B " + (rgb & 0x000000FF));
                    }
                    return rgb;
                }

            }
        };
        final ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
        return Toolkit.getDefaultToolkit().createImage(ip);
    }
    private BufferedImage merge(Image m1,Image m2,int basewidth, int baseHeight, int overX, int overY) {
        BufferedImage mergedImage = new BufferedImage(basewidth, baseHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = (Graphics2D) mergedImage.getGraphics();
        graphics.setBackground(Color.WHITE);
        graphics.drawImage(m1, 0, 0, null);
        if( drawHeight == 0 || drawWidth == 0)
            graphics.drawImage(makeColorTransparent( m2, Color.WHITE), overX,overY, null);
        else
            graphics.drawImage(makeColorTransparent( m2, Color.WHITE), overX,overY, drawWidth, drawHeight, null);
        return mergedImage;

    }

    private BufferedImage merge(Image m1,String target,int basewidth, int baseHeight, int overX, int overY){
        BufferedImage mergedImage = new BufferedImage(basewidth, baseHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = (Graphics2D) mergedImage.getGraphics();

        graphics.setBackground(Color.WHITE);

        graphics.drawImage(m1, 0, 0, null);

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setFont(new Font(fontFace, Font.BOLD , textSize));

        Graphics2D g2d = graphics;
        AffineTransform transform = g2d.getTransform();
        transform.translate(overX, overY+textSize);
        g2d.transform(transform);
        g2d.setColor(Color.black);
        FontRenderContext frc = g2d.getFontRenderContext();
        TextLayout tl = new TextLayout(target, g2d.getFont().deriveFont(textSize), frc);
        Shape shape = tl.getOutline(null);
        g2d.setStroke(new BasicStroke(textBorder));
        g2d.draw(shape);
        g2d.setColor(textColor);
        g2d.fill(shape);

        return mergedImage;
    }

    private void showError(String msg){
        log(msg);
        System.exit(-1);
    }

    static String stripExtension (String str) {
        if (str == null) return null;
        int pos = str.lastIndexOf(".");
        if (pos == -1) return str;
        return str.substring(0, pos);
    }

    public void merge() throws Exception{
        String baseImageName =  getValue("base");

        if( baseImageName == null)
            showError("Can't find Base Image files");

        File f = new File(baseImageName);
        if( !f.exists()){
            showError("Can't find "+ baseImageName );
        }

        BufferedImage baseImage = ImageIO.read(new File(getValue("base")));
        int overlayX = Integer.parseInt( getValue("x") );
        int overlayY = Integer.parseInt( getValue("y") );
        int width = baseImage.getWidth();
        int height = baseImage.getHeight();
        log("///// task start ////");


        if( isText ) {
            for (int i = 0; i < targets.size(); i++) {
                String s = targets.get(i).toString();
                try {
                    BufferedImage rstImage = merge(baseImage, s, width, height, overlayX, overlayY);
                    ImageIO.write(rstImage, outputFormat,
                            new File(targetPath + File.separator + (stripExtension(baseImageName) + "_" + s) + "." + outputFormat));
                    if( genVideo )
                        makeVideo(targetPath + File.separator + ( stripExtension(baseImageName)+"_"+ s) );
                    log(s + " convert complete...");
                } catch (Exception e) {
                    continue;
                }
            }
        }

        else{
            String[] overlayList = null;
            try {
                overlayList = new File(getValue("overlayDir")).list();
            }catch (Exception e){
                showError("Invalid overlay");
            }

            for (String of : overlayList) {
                try {
                    if( !of.toUpperCase().endsWith("JPG") && !of.toUpperCase().endsWith("PNG"))
                        continue;

                    BufferedImage oi = ImageIO.read(new File(getValue("overlayDir") + File.separator + of));
                    BufferedImage rstImage = merge(baseImage, oi, width, height, overlayX, overlayY);
                    ImageIO.write(rstImage, outputFormat,
                            new File(targetPath + File.separator + ( stripExtension(baseImageName)+"_"+ stripExtension(of))+ "."+outputFormat ));
                    if( genVideo )
                        makeVideo(targetPath + File.separator + ( stripExtension(baseImageName)+"_"+ stripExtension(of)) );
                    log(of+" convert completed");
                }catch(Exception e){
                    continue;
                }
            }
        }

        log("///// 작업 완료 ////");
    }

    public ImageMerger(String confPath) {
        configPath = confPath;

    }

    public void makeDefaultCfg(){
        File f = new File(CONFIG_PATH);
        try {
            FileWriter fw = new FileWriter(f);
            String str = "{\n" +
                    "  \"outputDir\":\"output\",\n" +
                    "  \"outputFormat\":\"jpg\",\n" +
                    "  \"mode\":\"text\",\n" +
                    "  \"overlayDir\":\"overlay\",\n" +
                    "  \"drawWidth\":200,\n" +
                    "  \"drawHeight\":200,\n" +
                    "  \"x\":62,\n" +
                    "  \"y\":200,\n" +

                    "  \"textSize\":100,\n" +
                    "  \"textBorder\":10,\n" +
                    "  \"base\":\"base.jpg\",\n" +
                    "  \"font\":\"궁서체\",\n" +
                    "  \"fontColor\":\"0,255,0\",\n" +
                    "  \"targets\":[\n" +
                    "    \"창신동\",\"후암동\",\"송파동\",\"길음동\",\"청량리\",\"철산동\",\"내손동\"\n" +
                    "  ]\n" +
                    "}";
            fw.write(str);
            fw.close();
        }catch(Exception e){}
    }

    public void makeVideo(String imgName){
        String filePath = targetPath;
        File fileP = new File(filePath);
        String commands = "ffmpeg.exe -nostats -loglevel 0 -y -loop 1 -i "+imgName+"."+outputFormat+" -c:v libx264 -t 5 -pix_fmt yuv420p -vf scale=1920:1080 "+imgName+".mp4";
        System.out.println(commands);
        try {
            Process p = Runtime.getRuntime().exec(commands);
            p.waitFor();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ImageMerger ig = new ImageMerger(CONFIG_PATH);
        try{
            ig.loadConfig();
        }catch(Exception e){
            ig.makeDefaultCfg();
            ig.showError("Can't find config files. make default config file");
        }

        try {
            ig.merge();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
