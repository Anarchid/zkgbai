package zkgbai.kgbutil;

/**
 * Created by haplo on 12/22/2015.
 */
public class ArrayGraphics {
    short[] data;
    int width;
    int height;

    public ArrayGraphics(int w, int h){
        width = w;
        height = h;
        data = new short[width * height];
    }

    public void clear(){
        for (int i = 0; i < data.length; i++){
            data[i] = 0;
        }
    }

    public void paintCircle(int cx, int cy, int radius, int intensity){
        int beginX = Math.max(cx - radius + 1, 0);
        int endX =   Math.min(cx + radius, width);

        int beginY = Math.max(cy - radius + 1, 0);
        int endY =   Math.min(cy + radius, height);

        int radsq = radius * radius;

        for (int x = beginX; x < endX; x++) {
            int dX = (cx - x);
            int dxSq = dX*dX;

            for (int y = beginY; y < endY; y++) {
                int dY = (cy - y);
                int dySq = dY*dY;

                int sum = dxSq + dySq;
                int index = (y * width) + x;

                if(sum <= radsq) {
                    data[index] += intensity;
                    if (data[index] < 0){
                        data[index] = Short.MAX_VALUE;
                    }
                }
            }
        }
    }

    public void unpaintCircle(int cx, int cy, int radius, int intensity){
        int beginX = Math.max(cx - radius + 1, 0);
        int endX =   Math.min(cx + radius, width);

        int beginY = Math.max(cy - radius + 1, 0);
        int endY =   Math.min(cy + radius, height);

        int radsq = radius * radius;

        for (int x = beginX; x < endX; x++) {
            int dX = (cx - x);
            int dxSq = dX*dX;

            for (int y = beginY; y < endY; y++) {
                int dY = (cy - y);
                int dySq = dY*dY;

                int sum = dxSq + dySq;
                int index = (y * width) + x;

                if(sum <= radsq) {
                    data[index] = (short) Math.max(data[index] - intensity, 0);
                }
            }
        }
    }

    public float getValue(int x, int y){
        float val;
        try{
            val = data[x + (y * width)];
        }catch (Exception e){
            return 0;
        }
        return val;
    }
}
