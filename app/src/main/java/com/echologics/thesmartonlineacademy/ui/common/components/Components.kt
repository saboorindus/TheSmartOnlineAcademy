package com.echologics.thesmartonlineacademy.ui.common.components


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.echologics.thesmartonlineacademy.ui.common.theme.Gray100
import com.echologics.thesmartonlineacademy.ui.common.theme.Purple
import com.echologics.thesmartonlineacademy.ui.common.theme.PurpleLight

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Purple)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun OutlinedPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Purple),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Purple)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun CountryPickerField(
    value: String,
    countries: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Country"
) {
    Box(modifier = modifier) {

        AppTextField(
            value = value,
            onValueChange = {},
            label = label,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // full area click to open dropdown
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { onExpandedChange(true) }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            countries.forEach { country ->
                DropdownMenuItem(
                    text = { Text(country) },
                    onClick = {
                        onValueChange(country)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    trailingIcon: @Composable (() -> Unit)? = null,

    // ✅ ADD THESE 3 (THIS FIXES YOUR ERRORS)
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation =
        if (isPassword) PasswordVisualTransformation()
        else VisualTransformation.None
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            isError = isError,
            singleLine = singleLine,
            maxLines = maxLines,
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Purple,
                focusedLabelColor = Purple,
                cursorColor = Purple
            )
        )

        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}


@Composable
fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PurpleLight,
            selectedLabelColor = Purple
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            selectedBorderColor = Purple,
            borderColor = Color(0xFFD3D1C7)
        )
    )
}

@Composable
fun StepProgressBar(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Step $currentStep of $totalSteps",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        LinearProgressIndicator(
            progress = { currentStep.toFloat() / totalSteps.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = Purple,
            trackColor = Gray100
        )
    }
}

@Composable
fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFCEBEB)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = message,
            color = Color(0xFFA32D2D),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(12.dp)
        )
    }
}