
package zkgbai.gui;


/**
 * Created by haplo on 11/19/2015.
 */

import java.awt.*;
import java.awt.image.*;

public class AdditiveCompositeContext implements CompositeContext {
    ColorModel srcColorModel;
    ColorModel dstColorModel;

    int ALPHA = 0xFF000000; // alpha mask
    int MASK7Bit = 0xFEFEFF; // mask for additive/subtractive shading

    public AdditiveCompositeContext(ColorModel srcColorModel, ColorModel dstColorModel) {
        this.srcColorModel = srcColorModel;
        this.dstColorModel = dstColorModel;
    }

    int add(int color1, int color2) {
        int pixel = (color1 & MASK7Bit) + (color2 & MASK7Bit);
        int overflow = pixel & 0x1010100;
        overflow = overflow - (overflow >> 8);
        return ALPHA | overflow | pixel;
    }


    public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
        Rectangle srcRect = src.getBounds();
        Rectangle dstInRect = dstIn.getBounds();
        Rectangle dstOutRect = dstOut.getBounds();
        int x = 0, y = 0;
        int w = Math.min(Math.min(srcRect.width, dstOutRect.width), dstInRect.width);
        int h = Math.min(Math.min(srcRect.height, dstOutRect.height), dstInRect.height);
        Object srcPix = null, dstPix = null;
        for (y = 0; y < h; y++)
            for (x = 0; x < w; x++) {
                srcPix = src.getDataElements(x + srcRect.x, y + srcRect.y, srcPix);
                dstPix = dstIn.getDataElements(x + dstInRect.x, y + dstInRect.y, dstPix);
                int sp = srcColorModel.getRGB(srcPix);
                int dp = dstColorModel.getRGB(dstPix);
                int rp = add(sp,dp);
                dstOut.setDataElements(x + dstOutRect.x, y + dstOutRect.y, dstColorModel.getDataElements(rp, null));
            }
    }

    public void dispose() {
    }
}

