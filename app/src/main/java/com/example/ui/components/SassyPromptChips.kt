package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ZoyaPersona
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.TextPrimary

@Composable
fun SassyPromptChips(
    onPromptSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp)
            .testTag("sassy_prompt_chips_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ZoyaPersona.QUICK_PROMPTS.forEachIndexed { index, prompt ->
            FilterChip(
                selected = false,
                onClick = { onPromptSelected(prompt) },
                label = {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = ObsidianCard,
                    labelColor = TextPrimary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = false,
                    borderColor = if (index % 2 == 0) NeonMagenta.copy(alpha = 0.5f) else NeonCyan.copy(alpha = 0.5f)
                ),
                modifier = Modifier.testTag("prompt_chip_$index")
            )
        }
    }
}
