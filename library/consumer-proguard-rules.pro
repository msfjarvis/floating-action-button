# keep getters/setters in RotatingDrawable so that animations can still work.
-keepclassmembers class me.msfjarvis.floatingactionbutton.FloatingActionsMenu$RotatingDrawable {
   void set*(***);
   *** get*();
}
