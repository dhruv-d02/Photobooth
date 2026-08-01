package com.dj.photobooth.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dj.photobooth.theme.PhotoboothColors
import com.dj.photobooth.theme.PhotoboothType
import kotlinx.coroutines.delay

/**
 * Floating pill toast - sparkle + confirmation copy, white pill on a dark or light screen,
 * bottom-anchored (design/handoff/README.md's Share screen: "toast on save/share: sparkle +
 * confirmation copy, auto-dismiss ~1.8s"; exact placement/timing from the dc.html: 88dp above
 * the screen's bottom edge, `pbPop` scale-in over 220ms, auto-dismiss at 1800ms). Shared by
 * Share and Gallery so both get the same confirmation moment.
 *
 * Owns its own dismiss timer (calls [onDismissRequest] once 1.8s elapses) rather than making
 * every caller re-implement the same `delay(1800)` - the caller just nulls out whatever state
 * holds [message] in response, the same way a Snackbar's auto-dismiss works.
 */
@Composable
fun BoxScope.Toast(
    message: String?,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier
            .align(Alignment.BottomCenter)
            .offset(y = (-88).dp),
        enter = scaleIn(
            initialScale = 0.8f,
            animationSpec = tween(220, easing = LinearOutSlowInEasing),
        ) + fadeIn(tween(220)),
        exit = fadeOut(tween(120)),
    ) {
        LaunchedEffect(message) {
            delay(1800)
            onDismissRequest()
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .background(PhotoboothColors.Paper, RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 9.dp),
        ) {
            Sparkle(size = 13.dp)
            Text(
                text = message.orEmpty(),
                style = PhotoboothType.display13(),
                color = PhotoboothColors.Ink,
            )
        }
    }
}
