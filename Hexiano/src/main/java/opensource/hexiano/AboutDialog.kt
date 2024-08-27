package opensource.hexiano

import android.app.Dialog
import android.content.Context

class AboutDialog(context: Context): Dialog(context)
{
    init {
        val versionName: String = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (ex: Exception) {
            "0.0a"
        }
        
        setContentView(R.layout.about)
        val name = context.getText(R.string.app_name)
        val versionStr = context.getText(R.string.version)
        val title = StringBuilder(name)
        title.append(" ").append(versionStr).append(" ").append(versionName)
        setTitle(title)
        
        // TextView licenseView = (TextView) findViewById(R.id.license)
        // CharSequence text = context.getText(R.string.license)
        // licenseView.setText(Html.fromHtml(text))
        // licenseView.setMovementMethod(LinkMovementMethod.getInstance())
    }
}
