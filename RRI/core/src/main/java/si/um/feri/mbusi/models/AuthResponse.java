package si.um.feri.mbusi.models;

public class AuthResponse {
  private User data;
  private String token;

  public User getData() {
    return data;
  }

  public void setData(User data) {
    this.data = data;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }
}
