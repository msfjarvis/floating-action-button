package me.msfjarvis.floatingactionbutton.sample

import android.app.Activity
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.android.material.floatingactionbutton.FloatingActionButton
import me.msfjarvis.floatingactionbutton.FloatingActionsMenu
import me.msfjarvis.floatingactionbutton.LabeledFloatingActionButton

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.pink_icon).setOnClickListener { Toast.makeText(this@MainActivity, "Clicked pink Floating Action Button", Toast.LENGTH_SHORT).show() }

        val button = findViewById<LabeledFloatingActionButton>(R.id.setter)
        button.size = FloatingActionButton.SIZE_MINI
        button.colorNormalResId = R.color.pink
        button.setImageDrawable(resources.getDrawable(R.drawable.ic_fab_star, theme))

        val actionB = findViewById<View>(R.id.action_b)

        val actionC = LabeledFloatingActionButton(baseContext)
        actionC.title = "Hide/Show Action above"
        actionC.setOnClickListener { actionB.visibility = if (actionB.visibility == View.GONE) View.VISIBLE else View.GONE }

        val menuMultipleActions = findViewById<FloatingActionsMenu>(R.id.multiple_actions)
        menuMultipleActions.addButton(actionC)

        val removeAction = findViewById<LabeledFloatingActionButton>(R.id.button_remove)
        removeAction.setOnClickListener { (findViewById<View>(R.id.multiple_actions_down) as FloatingActionsMenu).removeButton(removeAction) }

        val drawable = ShapeDrawable(OvalShape())
        drawable.paint.color = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) resources.getColor(R.color.white, theme) else resources.getColor(R.color.white)

        (findViewById<View>(R.id.setter_drawable) as LabeledFloatingActionButton).setImageDrawable(drawable)

        val actionA = findViewById<LabeledFloatingActionButton>(R.id.action_a)
        actionA.setOnClickListener { actionA.title = "Action A clicked" }

        // Test that FAMs containing FABs with visibility GONE do not cause crashes
        findViewById<View>(R.id.button_gone).visibility = View.GONE

        val actionEnable = findViewById<LabeledFloatingActionButton>(R.id.action_enable)
        actionEnable.setOnClickListener { menuMultipleActions.isEnabled = !menuMultipleActions.isEnabled }

        val rightLabels = findViewById<FloatingActionsMenu>(R.id.right_labels)
        val addedOnce = LabeledFloatingActionButton(this)
        addedOnce.title = "Added once"
        rightLabels.addButton(addedOnce)

        val addedTwice = LabeledFloatingActionButton(this)
        addedTwice.title = "Added twice"
        rightLabels.addButton(addedTwice)
        rightLabels.removeButton(addedTwice)
        rightLabels.addButton(addedTwice)
    }
}
