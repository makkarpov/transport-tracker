package ru.makkarpov.ttdroid.utils

/**
  * Created by user on 8/12/16.
  */
abstract class SimpleCached[K, V] {
  protected def calculate(k: K): V

  private var lastK: K = _
  private var lastV: V = _

  def apply(k: K): V = {
    if (lastK != k) {
      lastK = k
      lastV = calculate(k)
    }

    lastV
  }
}
