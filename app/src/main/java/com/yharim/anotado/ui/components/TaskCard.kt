package com.yharim.anotado.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yharim.anotado.R
import com.yharim.anotado.ui.home.TaskItemUiState

@Composable
fun TaskCard(task: TaskItemUiState, updating: Boolean, enabled: Boolean,
             onClick: () -> Unit, onToggleCompleted: () -> Unit) {
    val green = Color(0xFF1D7958)
    val badgeColor = when (task.priority) {
        5 -> Color(0xFFFBE7E5); 4 -> Color(0xFFFFEDDD); 3 -> Color(0xFFFFF5D6); else -> Color(0xFFE7F4EE)
    }
    val badgeText = when (task.priority) {
        5 -> Color(0xFFA34848); 4 -> Color(0xFFA56533); 3 -> Color(0xFF8A762D); else -> green
    }
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color(0xFFE0E8E3)),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            if (updating) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = green)
                }
            } else {
                IconToggleButton(checked = task.completed, onCheckedChange = { onToggleCompleted() }, enabled = enabled) {
                    Surface(Modifier.size(23.dp), shape = CircleShape,
                        color = if (task.completed) green else Color.Transparent,
                        border = BorderStroke(1.8.dp, green)) {
                        if (task.completed) Icon(painterResource(R.drawable.ic_check), "Marcar como pendiente", Modifier.padding(4.dp), tint = Color.White)
                        else Icon(painterResource(R.drawable.ic_check), "Marcar como completada", tint = Color.Transparent)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f).padding(top = 6.dp)) {
                Surface(color = badgeColor, shape = RoundedCornerShape(7.dp)) {
                    Text(task.priorityLabel, Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        color = badgeText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                Text(task.title, color = Color(0xFF203D33), fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None)
                Spacer(Modifier.height(12.dp))
                Text(task.category, color = Color(0xFF7D8D87), fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_calendar), null, Modifier.size(14.dp), tint = Color(0xFF7D8D87))
                    Spacer(Modifier.width(6.dp))
                    Text(task.dateLabel, color = Color(0xFF7D8D87), fontSize = 12.sp)
                }
            }
            Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.padding(top = 10.dp).size(16.dp), tint = Color(0xFFA4B0AA))
        }
    }
}
