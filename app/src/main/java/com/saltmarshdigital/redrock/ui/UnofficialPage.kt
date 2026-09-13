package com.saltmarshdigital.redrock.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saltmarshdigital.redrock.R
import com.saltmarshdigital.redrock.ui.theme.RR

@Composable
fun UnofficialPage() {
  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 36.dp, vertical = 24.dp),
  ) {
    Text(
      text = stringResource(R.string.unofficial_title),
      color = RR.Ink,
      fontSize = 26.sp,
      fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(18.dp))
    Text(
      text = stringResource(R.string.unofficial_body),
      color = RR.Ink,
      fontSize = 15.sp,
      lineHeight = 22.sp,
    )
  }
}
