import SwiftUI

struct FilterInputField: View {
    let icon: String
    let placeholder: String
    @Binding var text: String
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundColor(.white)
                .frame(width: 24, height: 24)
            
            TextField(placeholder, text: $text)
                .foregroundColor(.white)
                .font(.system(size: 16))
        }
        .padding(.horizontal, 16)
        .frame(height: 58)
        .background(Color(hex: "42818181"))
        .cornerRadius(12)
    }
}

struct FilterPickerField: View {
    let icon: String
    let placeholder: String
    @Binding var selectedValue: String
    let options: [String]
    let onSelect: (String) -> Void
    
    var body: some View {
        Menu {
            ForEach(options, id: \.self) { option in
                Button(action: {
                    selectedValue = option
                    onSelect(option)
                }) {
                    HStack {
                        Text(option)
                        if selectedValue == option {
                            Image(systemName: "checkmark")
                        }
                    }
                }
            }
        } label: {
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .foregroundColor(.white)
                    .frame(width: 24, height: 24)
                
                Text(selectedValue.isEmpty ? placeholder : selectedValue)
                    .foregroundColor(selectedValue.isEmpty ? Color(hex: "CBD5E1") : .white)
                    .font(.system(size: 16))
                
                Spacer()
                
                Image(systemName: "chevron.down")
                    .foregroundColor(.white.opacity(0.6))
                    .font(.system(size: 12))
            }
            .padding(.horizontal, 16)
            .frame(height: 58)
            .background(Color(hex: "42818181"))
            .cornerRadius(12)
        }
    }
}

