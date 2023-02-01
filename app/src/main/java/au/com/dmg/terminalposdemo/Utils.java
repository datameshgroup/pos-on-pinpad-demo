package au.com.dmg.terminalposdemo;

import android.content.Context;
import android.graphics.Bitmap;
import com.izettle.html2bitmap.Html2Bitmap;
import com.izettle.html2bitmap.content.WebViewContent;
import org.apache.commons.lang3.StringUtils;
import java.io.IOException;
import java.io.InputStream;


public class Utils {
    static Bitmap generateReceiptBitmap(Context context, String receiptData, Boolean isApproved) throws IOException {
        Bitmap receiptBitmap = null;
        String htmlStart =
                "<html><head><style>body { font-size: 18pt;font-family: 'Calibri', Helvetica, Arial, sans-serif;  }</style></head><body>";
        String htmlEnd = "</body></html>";
        String status = "";
        if(isApproved) {
            status = "<br /><center><b> Approved </b></center>";
        }
        else{
            status = "<br /><center><b> Declined </b></center>";
        }


        String htmlData = receiptData;
        String merchantCopyDelimiter = "*** MERCHANT COPY ***";
        String paddingAfter = "<p></p><br><br><p></p><p>-------------------------</p>";

        htmlData = htmlData.replaceAll("(\r\n|\n)", "<br />");

        if(htmlData.contains(merchantCopyDelimiter)){
            String substringBefore = StringUtils.substringBefore(htmlData, merchantCopyDelimiter);
            String substringAfter =  StringUtils.substringAfter(htmlData, merchantCopyDelimiter);
            htmlData = htmlStart + "<center> <font size=\"4\">" +
                    substringBefore + "</font><br /><b>" +
                    merchantCopyDelimiter + "</b></center>" +
                    substringAfter + status +
                    paddingAfter +
                    htmlEnd;
        }
        else{
            htmlData = htmlStart + htmlData + paddingAfter + htmlEnd;
        }

        try {
            receiptBitmap = new Html2Bitmap.Builder(context, WebViewContent.html(htmlData))
                    .setBitmapWidth(384)
                    .build()
                    .getBitmap();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return receiptBitmap;
    }

    static byte[] readAssetsFile(Context context, String fileName) throws IOException {
        InputStream input = null;
        try {
            input = context.getAssets().open(fileName);
            byte[] buffer = new byte[input.available()];
            input.read(buffer);
            return buffer;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
