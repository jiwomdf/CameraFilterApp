package com.katilijiwoadiwiyono.filterrecord.utils.filterutil

import com.katilijiwoadiwiyono.filterrecord.data.model.Vec3
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter

class GPUImageColorFilter(
    fragmentShader: String
): GPUImageFilter(
    NO_FILTER_VERTEX_SHADER, fragmentShader
) {

    companion object {
        private var vec3: Vec3? = null
        fun init(
            vec3: Vec3
        ): GPUImageColorFilter {
            this.vec3 = vec3
            val colorFragmentShader = """
                varying highp vec2 textureCoordinate;
                uniform sampler2D inputImageTexture;
                uniform lowp float brightness;
                
                void main()
                {
                    lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);
                    gl_FragColor = vec4((textureColor.rgb + vec3(${Companion.vec3?.x}, ${Companion.vec3?.y}, ${Companion.vec3?.z})), textureColor.w);
                }
            """.trimIndent()
            return GPUImageColorFilter(colorFragmentShader)
        }
    }

}