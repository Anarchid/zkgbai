
package zkgbai.military;


/**
 * Created by haplo on 11/19/2015.
 *
 */

import java.awt.*;
import java.awt.image.*;
public class AdditiveComposite implements Composite {
    public AdditiveComposite() {
        super();
    }
    public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints _) {
        return new AdditiveCompositeContext(srcColorModel,dstColorModel);
    }

}