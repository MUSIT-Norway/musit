export const resolveConditions = (condition1, block1, condition2, block2, blockElse) => {
  if (condition1) {
    return block1
  }
  if (condition2) {
    return block2
  }
  return blockElse
}
