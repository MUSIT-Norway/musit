 import { assert } from '../../../../../test/setup'
 import { validate } from '../validators'

 const numberTestProps = ({
   id: 'Test_1',
   validate: 'number',
   minimumLength: 2,
   maximumLength: 5
 })

 describe('Vaidation', () => {
   it('Number validation', () => {
     const naturalFormat = { ...numberTestProps, value: '12', precision: 0 }
     const naturalWithErrorFormat = { ...numberTestProps, value: '12,', precision: 0 }
     const naturalWithDescimalFormat = { ...numberTestProps, value: '12,1', precision: 0 }
     const defaultFormat = { ...numberTestProps, value: '12,123', precision: 3 }
     const wrongFormat = { ...numberTestProps, value: ',123', precision: 3 }
     const negativeNumberFormat = { ...numberTestProps, value: '-12,123', precision: 3 }
     const toManyDescimalsFormat = { ...numberTestProps, value: '12,1234', precision: 3 }
     const toBigNumberFormat = { ...numberTestProps, value: '123456', precision: 3 }
     const toSmallNumberFormat = { ...numberTestProps, value: '1', precision: 3 }
     const emptyNumberFormat = { ...numberTestProps, value: '', precision: 3 }
     const emptyPassingNumberFormat = { ...numberTestProps, value: '', minimumLength: 0, precision: 3 }

     assert.equal(validate(naturalFormat), 'success', 'naturalFormat is not working.')
     assert.equal(validate(naturalWithErrorFormat), 'error', 'naturalWithErrorFormat is working.')
     assert.equal(validate(naturalWithDescimalFormat), 'error', 'naturalWithDescimalFormat is working.')
     assert.equal(validate(defaultFormat), 'success', 'defaultFormat is not working.')
     assert.equal(validate(wrongFormat), 'error', 'defaultFormat is working.')
     assert.equal(validate(negativeNumberFormat), 'success', 'negativeNumberFormat is not working.')
     assert.equal(validate(toManyDescimalsFormat), 'error', 'toManyDescimalsFormat is working.')
     assert.equal(validate(toBigNumberFormat), 'error', 'toBigNumberFormat is working.')
     assert.equal(validate(toSmallNumberFormat), 'error', 'toSmallNumberFormat is working.')
     assert.equal(validate(emptyNumberFormat), 'error', 'emptyNumberFormat is working.')
     assert.equal(validate(emptyPassingNumberFormat), 'success', 'emptyPassingNumberFormat is not working.')
     assert.equal(validate(numberTestProps), 'error', 'missing important props is working.')
   })
 })
