import { describe, expect, it } from "vitest";
import { numberToVietnamese } from "./vietnamese-number.js";

describe("numberToVietnamese — units 0..9", () => {
  it.each([
    [0, "không"],
    [1, "một"],
    [2, "hai"],
    [3, "ba"],
    [4, "bốn"],
    [5, "năm"],
    [6, "sáu"],
    [7, "bảy"],
    [8, "tám"],
    [9, "chín"],
  ])("%i → %s", (n, expected) => {
    expect(numberToVietnamese(n)).toBe(expected);
  });
});

describe("numberToVietnamese — teens 10..19 (mười …, with mười lăm exception)", () => {
  it.each([
    [10, "mười"],
    [11, "mười một"],
    [12, "mười hai"],
    [13, "mười ba"],
    [14, "mười bốn"],
    [15, "mười lăm"],
    [16, "mười sáu"],
    [17, "mười bảy"],
    [18, "mười tám"],
    [19, "mười chín"],
  ])("%i → %s", (n, expected) => {
    expect(numberToVietnamese(n)).toBe(expected);
  });
});

describe("numberToVietnamese — 20..90 (mốt and lăm exceptions, ten-only forms)", () => {
  it.each([
    [20, "hai mươi"],
    [21, "hai mươi mốt"],
    [22, "hai mươi hai"],
    [25, "hai mươi lăm"],
    [29, "hai mươi chín"],
    [30, "ba mươi"],
    [31, "ba mươi mốt"],
    [40, "bốn mươi"],
    [45, "bốn mươi lăm"],
    [55, "năm mươi lăm"],
    [61, "sáu mươi mốt"],
    [70, "bảy mươi"],
    [81, "tám mươi mốt"],
    [85, "tám mươi lăm"],
    [90, "chín mươi"],
  ])("%i → %s", (n, expected) => {
    expect(numberToVietnamese(n)).toBe(expected);
  });
});

describe("numberToVietnamese — out of range falls back to String(n)", () => {
  it.each([
    [-1, "-1"],
    [91, "91"],
    [100, "100"],
    [0.5, "0.5"],
    [Number.NaN, "NaN"],
  ])("%p → %p", (n, expected) => {
    expect(numberToVietnamese(/** @type {number} */ (n))).toBe(expected);
  });
});
