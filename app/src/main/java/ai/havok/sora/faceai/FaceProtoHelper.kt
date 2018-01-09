package ai.havok.sora.faceai

import com.google.android.gms.vision.face.Face

/**
* This object is used to transform and interact with the FaceProto.
*/

object FaceProtoHelper {
    fun convertFaceToProto(face: Face, userFaceState: FaceState = FaceState.NONE) =
        ai.havok.sora.faceai.Face.newBuilder()
                .setAiInfo(AiInfo.newBuilder()
                        .setLeftEyeOpen(face.isLeftEyeOpenProbability)
                        .setRightEyeOpen(face.isRightEyeOpenProbability)
                        .setIsSmiling(face.isSmilingProbability)
                ).setOrientation(Orientation.newBuilder()
        ).setUserInput(UserInput.newBuilder()
                .setFaceState(userFaceState)
        ).build()
}