package com.example.myaugmentedfaceapp

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.ArCoreApk
import com.google.ar.core.AugmentedFace
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.Texture
import com.google.ar.sceneform.ux.AugmentedFaceNode


class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val MIN_OPENGL_VERSION = 3.0
    }

    private lateinit var arFragment: FaceArFragment
    private lateinit var modelRenderable: ModelRenderable
    private lateinit var texture: Texture

    private val faceNodeMap: HashMap<AugmentedFace, AugmentedFaceNode> = HashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!checkIsSupportedDeviceOrFinish(this))
            return

        arFragment = supportFragmentManager.findFragmentById(R.id.face_fragment) as FaceArFragment

        ModelRenderable.builder()
            .setSource(this, R.raw.fox_face)
            .build()
            .thenAccept {
                modelRenderable = it
                it.isShadowCaster = false
                it.isShadowReceiver = false
            }


        Texture.builder()
            .setSource(this, R.drawable.fox_face_mesh_texture)
            .build()
            .thenAccept {
                texture = it
            }


        val sceneView = arFragment.arSceneView
        sceneView.cameraStreamRenderPriority = Renderable.RENDER_PRIORITY_FIRST

        val scene = sceneView.scene

        scene.addOnUpdateListener {

            val faceList = sceneView.session!!.getAllTrackables(AugmentedFace::class.java)

            for (face in faceList) {
                if (!faceNodeMap.containsKey(face)) {
                    val faceNode = AugmentedFaceNode(face)
                    faceNode.setParent(scene)
                    faceNode.faceRegionsRenderable = modelRenderable
                    faceNode.faceMeshTexture = texture
                    faceNodeMap[face] = faceNode
                }
            }

            val iter: MutableIterator<Map.Entry<AugmentedFace, AugmentedFaceNode>> =
                faceNodeMap.entries.iterator()

            while (iter.hasNext()) {
                val entry = iter.next()
                val face = entry.key

                if (face.trackingState == TrackingState.STOPPED) {
                    val faceNode = entry.value
                    faceNode.setParent(null)
                    iter.remove()
                }
            }


        }
    }

    private fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {

        if (ArCoreApk.getInstance().checkAvailability(activity) == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
            Log.e(TAG, "Augmented Faces requires ArCore.")
            Toast.makeText(activity, "Augmented Faces requires ARCore", Toast.LENGTH_LONG).show()
            activity.finish()
            return false
        }

        val openGlVersionString =
            (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .deviceConfigurationInfo
                .glEsVersion

        if (openGlVersionString.toDouble() < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL.ES 3.0 later.")
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                .show()
            activity.finish()
            return false
        }

        return true

    }
}
