package com.hyperether.auth.microsoft

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperether.auth.library.generated.resources.Res
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt
import com.hyperether.auth.library.generated.resources.microsoft_icon

@Composable
fun MicrosoftSignInButton(
    modifier: Modifier = Modifier.height(44.dp),
    mode: MicrosoftButtonMode = MicrosoftButtonMode.WhiteWithOutline,
    text: String = "Sign in with Microsoft",
    shape: Shape = ButtonDefaults.shape,
    onClick: () -> Unit,
) {
    val buttonColor = getButtonColor(mode)
    val borderStroke = getBorderStroke(mode)
    val horizontalPadding = 0.dp
    val iconTextPadding = 8.dp
    var fontSize by remember { mutableStateOf(19) }
    var buttonHeight by remember { mutableStateOf(44) }
    var marginEnd by remember { mutableStateOf(0) }
    val localDensity = LocalDensity.current
    Button(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                val height =
                    with(localDensity) { coordinates.size.height.toDp().value.roundToInt() }
                val width = with(localDensity) { coordinates.size.width.toDp().value.roundToInt() }
                marginEnd = (width * 0.06).roundToInt()
                buttonHeight = height / 2
                fontSize = ((height * 0.43).roundToInt())
            }.defaultMinSize(minWidth = 140.dp, minHeight = 30.dp),
        contentPadding = PaddingValues(horizontal = horizontalPadding),
        onClick = onClick,
        shape = shape,
        colors = buttonColor,
        border = borderStroke,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.width(marginEnd.dp))
            MicrosoftIcon(modifier = Modifier.size(buttonHeight.dp))
            Spacer(modifier = Modifier.width(iconTextPadding))
            Text(
                modifier = Modifier
                    .padding(end = marginEnd.dp),
                text = text,
                fontSize = fontSize.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MicrosoftIcon(modifier: Modifier = Modifier) {
    Image(
        modifier = modifier,
        painter = painterResource(Res.drawable.microsoft_icon),
        contentDescription = "microsoftIcon"
    )
}

private fun getBorderStroke(mode: MicrosoftButtonMode): BorderStroke? {
    val borderStroke = when (mode) {
        MicrosoftButtonMode.WhiteWithOutline -> BorderStroke(
            width = 1.dp,
            color = Color.Black,
        )

        else -> null
    }
    return borderStroke
}

@Composable
private fun getButtonColor(mode: MicrosoftButtonMode): ButtonColors {
    val containerColor = when (mode) {
        MicrosoftButtonMode.Black -> Color.Black
        else -> Color.White
    }

    val contentColor = when (mode) {
        MicrosoftButtonMode.Black -> Color.White
        else -> Color.Black
    }

    return ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor)
}

sealed interface MicrosoftButtonMode {
    data object Black : MicrosoftButtonMode
    data object White : MicrosoftButtonMode
    data object WhiteWithOutline : MicrosoftButtonMode
}