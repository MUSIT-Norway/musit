const validateString = (value, minimumLength = 1, maximumLength = 20) => {
  const isSomething = value && value.length >= minimumLength
  const isValid = isSomething ? 'success' : null
  return value && value.length > maximumLength ? 'error' : isValid
}

const validateNumber = (value, minimumLength = 1) => {
  const isSomething = value && value.length >= minimumLength
  const isValid = isSomething ? 'success' : null
  return isSomething && !(/^-?\d+,\d+$/.test(value) || /^-?\d+$/.test(value)) ? 'error' : isValid
}

const validate = (source) => {
  let lValue = ''
  if (source.validator) {
    lValue = source.validator
  } else {
    switch (source.validate) {
      case 'text':
        lValue = validateString(source.value, source.minimumLength, source.maximumLength)
        break
      case 'number':
        lValue = validateNumber(source.value, source.minimumLength)
        break
      default:
        lValue = null
    }
  }
  return lValue
}

export { validate }
