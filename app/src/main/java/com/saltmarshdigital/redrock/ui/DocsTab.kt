package com.saltmarshdigital.redrock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saltmarshdigital.redrock.R
import com.saltmarshdigital.redrock.ui.theme.RR

private data class DocChapter(val title: Int, val body: Int)

private val Chapters =
  listOf(
    DocChapter(R.string.docs_ch1_title, R.string.docs_ch1_body),
    DocChapter(R.string.docs_ch2_title, R.string.docs_ch2_body),
    DocChapter(R.string.docs_ch3_title, R.string.docs_ch3_body),
    DocChapter(R.string.docs_ch4_title, R.string.docs_ch4_body),
    DocChapter(R.string.docs_ch5_title, R.string.docs_ch5_body),
    DocChapter(R.string.docs_ch6_title, R.string.docs_ch6_body),
    DocChapter(R.string.docs_ch7_title, R.string.docs_ch7_body),
    DocChapter(R.string.docs_ch8_title, R.string.docs_ch8_body),
    DocChapter(R.string.docs_ch9_title, R.string.docs_ch9_body),
    DocChapter(R.string.docs_ch10_title, R.string.docs_ch10_body),
    DocChapter(R.string.docs_ch11_title, R.string.docs_ch11_body),
    DocChapter(R.string.docs_ch12_title, R.string.docs_ch12_body),
    DocChapter(R.string.docs_ch13_title, R.string.docs_ch13_body),
  )

@Composable
fun DocsTab() {
  var chapter by remember { mutableIntStateOf(0) }
  val current = Chapters[chapter]
  Row(
    modifier = Modifier.fillMaxSize().padding(start = 20.dp, end = 24.dp, top = 16.dp, bottom = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    Column(
      modifier =
        Modifier.width(200.dp)
          .fillMaxHeight()
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text(
        text = stringResource(R.string.docs_title),
        color = RR.Ink,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Chapters.forEachIndexed { index, item ->
        val selected = index == chapter
        Text(
          text = stringResource(item.title),
          color = if (selected) RR.Paper else RR.Ink,
          fontSize = 14.sp,
          fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
          modifier =
            Modifier.fillMaxWidth()
              .clip(RR.Shape)
              .background(if (selected) RR.Ember else RR.Chip)
              .clickable(role = Role.Tab, onClick = { chapter = index })
              .padding(horizontal = 12.dp, vertical = 10.dp),
        )
      }
    }
    Column(
      modifier =
        Modifier.fillMaxHeight()
          .weight(1f)
          .verticalScroll(rememberScrollState()),
    ) {
      Text(
        text = stringResource(current.title),
        color = RR.Ink,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = stringResource(current.body),
        color = RR.Ink,
        fontSize = 15.sp,
        lineHeight = 22.sp,
      )
    }
  }
}
