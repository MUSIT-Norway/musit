export const resolveConditions = (condition1, block1, condition2, block2, blockElse) => {
  if (condition1) {
    return block1
  }
  if (condition2) {
    return block2
  }
  return blockElse
}

export const flatten = (arr) => {
  const obj = {};

  for (let i = 0; i < arr.length; i++) {
    Object.keys(arr[i]).forEach((x) => {
      obj[x] = arr[i][x]
    })
  }

  return obj
}
